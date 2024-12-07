package io.github.chromonym.playercontainer.containers;

import io.github.chromonym.playercontainer.PlayerContainer;

public class ContainerInstance<C extends AbstractContainer> {
    
    private final C container;
    private int ID = 0;

    public ContainerInstance(C container) {
        this(container, PlayerContainer.getNextAvailableContainerID());
    }

    public ContainerInstance(C container, int id) {
        this.container = container;
        this.ID = id;
        if (id != 0) {
            PlayerContainer.LOGGER.info("Container with ID "+Integer.toString(id)+" created");
            if (PlayerContainer.containers.containsKey(id)) {
                PlayerContainer.LOGGER.warn("Container ID "+Integer.toString(id)+" already exists!");
            } else {
                PlayerContainer.containers.put(id, this);
            }
        }
    }

    public int getID() {
        /*if (PlayerContainer.containers.containsValue(this)) {
            int trackedID = PlayerContainer.containers.inverse().get(this);
            if (ID != trackedID) {
                PlayerContainer.LOGGER.warn("Tracked ID not the same as stored ID for container "+Integer.toString(trackedID)+" or "+Integer.toString(ID)+"!");
                ID = trackedID;
            }
        } else {
            if (ID != 0) {
                PlayerContainer.LOGGER.warn("Could not get container ID! Generating new ID instead.");
            }
            int newID = PlayerContainer.getNextAvailableContainerID();
            ID = newID;
            PlayerContainer.containers.put(newID,this);
        }*/
        return ID;
    }

    public C getContainer() {
        return container;
    }

    // write methods in here that call the relevant AbstractContainer method

    public void onDestroy() {
        PlayerContainer.containers.remove(ID);
        container.onDestroy(this);
    }
}
