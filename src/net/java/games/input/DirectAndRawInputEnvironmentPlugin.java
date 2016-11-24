/**
 * Copyright (C) 2007 Jeremy Booth (jeremy@newdawnsoftware.com)
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer. Redistributions in binary 
 * form must reproduce the above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. 
 * The name of the author may not be used to endorse or promote products derived
 * from this software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO 
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package net.java.games.input;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 * Combines the list of seperate keyboards and mice found with the raw plugin,
 * with the game controllers found with direct input.
 * 
 * @author Jeremy
 */
public class DirectAndRawInputEnvironmentPlugin extends ControllerEnvironment implements ControllerListener {

	private RawInputEnvironmentPlugin rawPlugin;
	private DirectInputEnvironmentPlugin dinputPlugin;
	
	private List<Controller> controllersAdded;
	private List<Controller> controllersRemoved;
	
	private Map<String, Controller> allControllers;
	
	public DirectAndRawInputEnvironmentPlugin() {
		// These two *must* be loaded in this order for raw devices to work.
		rawPlugin = new RawInputEnvironmentPlugin();
		dinputPlugin = new DirectInputEnvironmentPlugin();
		controllersAdded = new LinkedList<Controller>();
		controllersRemoved = new LinkedList<Controller>();
		allControllers = new HashMap<String, Controller>();
	}

	/**
	 * @see net.java.games.input.ControllerEnvironment#isSupported()
	 */
	public boolean isSupported() {
		return rawPlugin.isSupported() || dinputPlugin.isSupported();
	}

	@Override
	public void controllerRemoved(ControllerEvent ev) {
		controllersRemoved.add(ev.getController());
	}

	@Override
	public void controllerAdded(ControllerEvent ev) {
		controllersAdded.add(ev.getController());
	}

	@Override
	protected void updateControllers() {
		dinputPlugin.updateControllers();
		rawPlugin.updateControllers();
		_updateControllers();
	}
	
	private void _updateControllers() {
		if (controllersRemoved.size() > 0 || controllersAdded.size() > 0) {
			// Get old set of controllers
			Controller[] previousControllers = getControllers();
			
			boolean hasRawKeyboard = true;
			boolean hasRawMouse = true;
			
			// Remove Controllers
			for (Controller controller : controllersRemoved) {
				if (controller instanceof RawKeyboard) {
					hasRawKeyboard = true;
				}
				if (controller instanceof RawMouse) {
					hasRawMouse = true;
				}
				allControllers.remove(controller.getInstanceIdentifier());
			}
			controllersRemoved.clear();
			
			// Add Controllers
			for (Controller controller : controllersAdded) {
				if (controller instanceof RawKeyboard) {
					hasRawKeyboard = true;
				}
				if (controller instanceof RawMouse) {
					hasRawMouse = true;
				}
				allControllers.put(controller.getInstanceIdentifier(), controller);
			}
			controllersAdded.clear();
			
			// Determine if Raw or Direct Input is used
			for (Controller controller : allControllers.values()) {
				if (controller instanceof RawKeyboard) {
					hasRawKeyboard = true;
				}
				if (controller instanceof RawMouse) {
					hasRawMouse = true;
				}
			}
			
			// Update external controllers
			synchronized(controllers) {
				controllers.clear();
				for (Controller controller : allControllers.values()) {
					if (!hasRawKeyboard && (controller instanceof DIKeyboard)) {
						continue;
					} else if (!hasRawMouse && (controller instanceof DIMouse)) {
						continue;
					} else {
						controllers.put(controller.getInstanceIdentifier(), controller);
					}
				}
			}
			
			// Get new set of Controllers
			Controller[] newControllers = getControllers();
			
			// Fire remove controllers events
			for (Controller previousController : previousControllers) {
				boolean hasValue = false;
				for (Controller newController : newControllers) {
					if (previousController.getInstanceIdentifier().equals(newController.getInstanceIdentifier())) {
						hasValue = true;
						break;
					}
				}
				if (!hasValue) {
					fireControllerRemoved(previousController);
				}
			}
			
			
			// Fire add controller events
			for (Controller newController : newControllers) {
				boolean hasValue = false;
				for (Controller previousController : previousControllers) {
					if (previousController.getInstanceIdentifier().equals(newController.getInstanceIdentifier())) {
						hasValue = true;
						break;
					}
				}
				if (!hasValue) {
					fireControllerAdded(newController);
				}
			}
		}
	}

	@Override
	protected void initialize() {
		rawPlugin.addControllerListener(this);
		dinputPlugin.addControllerListener(this);
		dinputPlugin.initialize();
		rawPlugin.initialize();
		_updateControllers();
	}

	@Override
	protected void destroy() {
		rawPlugin.removeControllerListener(this);
		dinputPlugin.removeControllerListener(this);
		rawPlugin.destroy();
		dinputPlugin.destroy();
	}
}
