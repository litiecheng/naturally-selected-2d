package net.mostlyoriginal.ns2d.component;

import com.artemis.Component;

/**
 * @author Daan van Yperen
 */
public class Physics extends Component {
    public float vx; // velocityX
    public float vy; // velocityY

    public boolean onFloor = false;
    public boolean onWall = false;
}