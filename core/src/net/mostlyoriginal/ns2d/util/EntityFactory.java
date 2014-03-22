package net.mostlyoriginal.ns2d.util;

import com.artemis.Entity;
import com.artemis.World;
import net.mostlyoriginal.ns2d.G;
import net.mostlyoriginal.ns2d.component.*;

/**
 * @author Daan van Yperen
 */
public class EntityFactory {

    public static Entity createPlayer(final World world, final float x, final float y) {

        Entity player = newPositioned(world, x, y)
                .addComponent(new Anim("player", Anim.Layer.PLAYER))
                .addComponent(new Physics())
                .addComponent(new Gravity())
                .addComponent(new PlayerControlled())
                .addComponent(new Bounds(G.CELL_SIZE, G.CELL_SIZE))
                .addComponent(new CameraFocus());
        return player;
    }

    public static Entity createSkulk(final World world, final float x, final float y) {

        return newPositioned(world, x, y)
                .addComponent(new Anim("skulk", Anim.Layer.ENEMIES))
                .addComponent(new Physics())
                .addComponent(new Gravity())
                .addComponent(new SkulkControlled())
                .addComponent(new Bounds(G.CELL_SIZE, G.CELL_SIZE));
    }

    public static Entity createSpawner(final World world, final float x, final float y) {
        return newPositioned(world, x, y)
                .addComponent(new Anim("spawner", Anim.Layer.DIRECTLY_BEHIND_PLAYER));

    }

    private static Entity newPositioned(final World world, final float x, final float y) {
        return world.createEntity()
                .addComponent(new Pos(x, y));
    }

    public static Entity createDuct(World world, float x, float y) {
        return world.createEntity()
                .addComponent(new Pos(x, y))
                .addComponent(new Bounds(G.CELL_SIZE, G.CELL_SIZE))
                .addComponent(new EntitySpawner("skulk"))
                .addComponent(new Anim("duct", Anim.Layer.ON_WALL));
    }

    public static Entity createBullet(World world, float x, float y) {
        return newPositioned(world, x, y)
                .addComponent(new Anim("bullet", Anim.Layer.BULLETS))
                .addComponent(new Physics())
                .addComponent(new Gravity())
                .addComponent(new Bounds(7,4));
    }

    public static Entity createPlayerArm(World world, float x, float y, Entity player) {
        return newPositioned(world, x, y)
                .addComponent(new Anim("player-arm", Anim.Layer.PLAYER_ARM))
                .addComponent(new Attached(player))
                .addComponent(new Bounds(G.CELL_SIZE, G.CELL_SIZE));
    }

    public static Entity createMouseCursor(World world, float x, float y) {
        return newPositioned(world, x, y)
                .addComponent(new MouseCursor());
    }
}