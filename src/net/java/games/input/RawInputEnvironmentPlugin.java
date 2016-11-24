/*
 * %W% %E%
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*****************************************************************************
 * Copyright (c) 2003 Sun Microsystems, Inc.  All Rights Reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistribution of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materails provided with the distribution.
 *
 * Neither the name Sun Microsystems, Inc. or the names of the contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANT OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMEN, ARE HEREBY EXCLUDED.  SUN MICROSYSTEMS, INC. ("SUN") AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS
 * A RESULT OF USING, MODIFYING OR DESTRIBUTING THIS SOFTWARE OR ITS 
 * DERIVATIVES.  IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES.  HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OUR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for us in
 * the design, construction, operation or maintenance of any nuclear facility
 *
 *****************************************************************************/
package net.java.games.input;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.util.plugins.Plugin;

/** DirectInput implementation of controller environment
 * @author martak
 * @author elias
 * @version 1.0
 */
public final class RawInputEnvironmentPlugin extends ControllerEnvironment implements Plugin {
	
	private static boolean supported = false;

	/**
	 * Static utility method for loading native libraries.
	 * It will try to load from either the path given by
	 * the net.java.games.input.librarypath property
	 * or through System.loadLibrary().
	 * 
	 */
	static void loadLibrary(final String lib_name) {
		AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					public final Object run() {
					    try {
    						String lib_path = System.getProperty("net.java.games.input.librarypath");
    						if (lib_path != null)
    							System.load(lib_path + File.separator + System.mapLibraryName(lib_name));
    						else
    							System.loadLibrary(lib_name);
					    } catch (UnsatisfiedLinkError e) {
					        e.printStackTrace();
					        supported = false;
					    }
						return null;
					}
				});
	}
    
	static String getPrivilegedProperty(final String property) {
	       return (String)AccessController.doPrivileged(new PrivilegedAction<Object>() {
	                public Object run() {
	                    return System.getProperty(property);
	                }
	            });
		}
		

	static String getPrivilegedProperty(final String property, final String default_value) {
       return (String)AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return System.getProperty(property, default_value);
                }
            });
	}
		
	static {
		String osName = getPrivilegedProperty("os.name", "").trim();
		if(osName.startsWith("Windows")) {
			supported = true;
			if("x86".equals(getPrivilegedProperty("os.arch"))) {
				loadLibrary("jinput-raw");
			} else {
				loadLibrary("jinput-raw_64");
			}
		}
	}
	
	private List<RawDevice> active_devices;
	private RawInputEventQueue queue;

	/** Creates new DirectInputEnvironment */
	public RawInputEnvironmentPlugin() {
		active_devices = new ArrayList<RawDevice>();
		queue = new RawInputEventQueue();
	}

	private final static SetupAPIDevice lookupSetupAPIDevice(String device_name, List<SetupAPIDevice> setupapi_devices) {
		/* First, replace # with / in the device name, since that
		 * seems to be the format in raw input device name
		 */
		device_name = device_name.replaceAll("#", "\\\\").toUpperCase();
		for (int i = 0; i < setupapi_devices.size(); i++) {
			SetupAPIDevice device = setupapi_devices.get(i);
			if (device_name.indexOf(device.getInstanceIdentifier().toUpperCase()) != -1)
				return device;
		}
		return null;
	}

	private final static native void enumerateDevices(RawInputEventQueue queue, List<RawDevice> devices) throws IOException;

	public boolean isSupported() {
		return supported;
	}

	/*
	 * The raw input API, while being able to access
	 * multiple mice and keyboards, is a bit raw (hah)
	 * since it lacks some important features:
	 *
	 * 1. The list of keyboards and the list of mice
	 *    both include useless Terminal Server
	 *    devices (RDP_MOU and RDP_KEY) that we'd
	 *    like to skip.
	 * 2. The device names returned by GetRawInputDeviceInfo()
	 *    are not for display, but instead synthesized
	 *    from a combination of a device instance id
	 *    and a GUID.
	 *
	 * A solution to both problems is the SetupAPI that allows
	 * us to enumerate all keyboard and mouse devices and fetch their
	 * descriptive names and at the same time filter out the unwanted
	 * RDP devices.
	 */
	private final static List<SetupAPIDevice> enumSetupAPIDevices() throws IOException {
		List<SetupAPIDevice> devices = new ArrayList<SetupAPIDevice>();
		nEnumSetupAPIDevices(getKeyboardClassGUID(), devices);
		nEnumSetupAPIDevices(getMouseClassGUID(), devices);
		return devices;
	}
	private final static native void nEnumSetupAPIDevices(byte[] guid, List<SetupAPIDevice> devices) throws IOException;

	private final static native byte[] getKeyboardClassGUID();
	private final static native byte[] getMouseClassGUID();

	@Override
	protected void updateControllers() {
		if(isSupported()) {
			try {
				_updateController();
			} catch (IOException e) {
				logln("Failed to enumerate devices: " + e.getMessage());
			}
		}
	}
	
	private List<RawDevice> getDevices() throws IOException {
		List<RawDevice> devices = new ArrayList<RawDevice>();
		enumerateDevices(queue, devices);
		return devices;
	}
	
	private List<Controller> getControllers(List<RawDevice> devices, List<SetupAPIDevice> setupapi_devices) throws IOException {
		List<Controller> controllers = new ArrayList<Controller>();
		for (int i = 0; i < devices.size(); i++) {
			RawDevice device = (RawDevice)devices.get(i);
			SetupAPIDevice setupapi_device = lookupSetupAPIDevice(device.getName(), setupapi_devices);
			if (setupapi_device == null) {
				/* Either the device is an RDP or we failed to locate the
				 * SetupAPI device that matches
				 */
				controllers.add(null);
			} else {
				RawDeviceInfo info = device.getInfo();
				controllers.add(info.createControllerFromDevice(device, setupapi_device));
			}
		}
		return controllers;
	}
	
	protected void _updateController() throws IOException {
		List<RawDevice> newDevices = getDevices();
		List<SetupAPIDevice> setupapi_devices = enumSetupAPIDevices();
		List<Controller> newControllers = getControllers(newDevices, setupapi_devices);
		Collection<Controller> toAdd = new LinkedList<Controller>();
		Collection<Controller> toRemove = new LinkedList<Controller>();
		List<RawDevice> toAddDevices = new LinkedList<RawDevice>();
		
		// Check for controllers which need to be removed
		for (Controller controller1 : controllers.values()) {
			boolean hasValue = false;
			String controller1Identifier = controller1.getInstanceIdentifier();
			for (Controller controller2 : newControllers) {
				if (controller2 != null && controller1Identifier.equals(controller2.getInstanceIdentifier())) {
					hasValue = true;
					break;
				}
			}
			if (!hasValue) {
				toRemove.add(controller1);
			}
		}
		
		// Check for controllers which need to be added
		for (int i = 0; i < newControllers.size(); i++) {
			Controller controller = newControllers.get(i);
			if (controller != null && !controllers.containsKey(controller.getInstanceIdentifier())) {
				toAdd.add(controller);
				toAddDevices.add(newDevices.get(i));
			}
		}
		
		// Add/Remove controllers
		synchronized(controllers) {
			for (Controller controller : toRemove) {
				controllers.remove(controller.getInstanceIdentifier());
			}
			for (Controller controller : toAdd) {
				controllers.put(controller.getInstanceIdentifier(), controller);
			}
		}
		
		// Fire remove controllers events
		for (Controller controller : toRemove) {
			fireControllerRemoved(controller);
		}
		
		// Fire add controller events
		for (Controller controller : toAdd) {
			fireControllerAdded(controller);
		}
		
		queue.start(toAddDevices);
		active_devices.addAll(toAddDevices);
	}

	@Override
	protected void initialize() {
		updateControllers();
	}

	@Override
	protected void destroy() {
		queue.stop();
	}

} // class DirectInputEnvironment
