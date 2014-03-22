package net.mostlyoriginal.ns2d.system.active;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.systems.EntityProcessingSystem;
import net.mostlyoriginal.ns2d.component.*;
import net.mostlyoriginal.ns2d.system.passive.AssetSystem;
import net.mostlyoriginal.ns2d.system.passive.CameraSystem;
import net.mostlyoriginal.ns2d.system.passive.MapSystem;
import net.mostlyoriginal.ns2d.util.MapMask;

/**
 * Constrain movement to map collision.
 *
 * Inteded to clamp physics calculations.
 *
 * @author Daan van Yperen
 */
@Wire
public class MapCollisionSystem extends EntityProcessingSystem {

    private static boolean DEBUG=false;

    private MapSystem mapSystem;
    private AssetSystem assetSystem;
    private CameraSystem cameraSystem;

    private boolean initialized;
    private MapMask solidMask;

    private ComponentMapper<Physics> ym;
    private ComponentMapper<Pos> pm;
    private ComponentMapper<Bounds> bm;

    public MapCollisionSystem() {
        super(Aspect.getAspectForAll(Physics.class, Pos.class, Bounds.class));
    }

    @Override
    protected void begin() {
        if ( !initialized )
        {
            initialized=true;
            solidMask = mapSystem.getMask("solid");
        }
    }

    @Override
    protected void end() {
    }

    @Override
    protected void process(Entity e) {
        final Physics physics = ym.get(e);
        final Pos pos = pm.get(e);
        final Bounds bounds = bm.get(e);

        //  no math required here.
        if ( physics.vx == 0 && physics.vy == 0 ) return;

        float px = pos.x + physics.vx * world.delta;
        float py = pos.y + physics.vy * world.delta;

        physics.onWall = physics.onFloor = false;

        if ( (physics.vx > 0 && collides(px + bounds.x2, py + bounds.y1 + (bounds.y2 - bounds.y1) * 0.5f)) ||
             (physics.vx < 0 && collides(px + bounds.x1, py + bounds.y1 + (bounds.y2 - bounds.y1) * 0.5f)) )
        {
            physics.onWall = true;
            physics.vx = 0;
            px = pos.x;
        }

        if ( (physics.vy > 0 && collides(px + bounds.x1 + (bounds.x2 - bounds.x1) * 0.5f, py + bounds.y2)) ||
             (physics.vy < 0 && collides(px + bounds.x1 + (bounds.x2 - bounds.x1) * 0.5f, py + bounds.y1)) )
        {
            if ( physics.vy < 0 ) physics.onFloor = true;
            physics.onWall = true;
            physics.vy = 0;
        }

    }

    private boolean collides(final float x, final float y) {
        if ( DEBUG )
        {
            world.createEntity()
                    .addComponent(new Pos(x-1,y-1))
                    .addComponent(new Anim("debug-marker"))
                    .addComponent(new Terminal(1))
                    .addToWorld();
        }

        return solidMask.atScreen(x,y);
    }
}