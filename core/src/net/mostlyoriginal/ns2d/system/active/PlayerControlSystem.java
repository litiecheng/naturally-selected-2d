package net.mostlyoriginal.ns2d.system.active;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import net.mostlyoriginal.ns2d.component.*;
import net.mostlyoriginal.ns2d.system.passive.AssetSystem;
import net.mostlyoriginal.ns2d.system.render.DialogRenderSystem;

/**
 * @author Daan van Yperen
 */
@Wire
public class PlayerControlSystem extends EntityProcessingSystem {

    public static final int MOVEMENT_FACTOR = 1000;
    public static final int JUMP_FACTOR = 10000;
    public static final int JETPACK_THRUST = 1500;
    public static final int THRUST_VECTOR = 90;
    public static final float ROTATIONAL_SPEED_JETPACK_ON = 120f;
    public static final float ROTATIONAL_SPEED_JETPACK_OFF = 360f;
    private ComponentMapper<Physics> ym;
    private ComponentMapper<WallSensor> wm;
    private ComponentMapper<Pos> pm;
    private ComponentMapper<Weapon> wem;
    private ComponentMapper<Gravity> gm;
    private ComponentMapper<Inventory> im;
    private ComponentMapper<Anim> am;
    private ComponentMapper<Frozen> fm;
    private ComponentMapper<Bounds> bm;
    private ComponentMapper<Attached> attm;

    private EntitySpawnerSystem entitySpawnerSystem;
    private PhysicsSystem physicsSystem;
    private AssetSystem assetSystem;
    private boolean jetpackLooping;
    private ParticleSystem particleSystem;
    private float jetpackGasCooldown;
    private Vector2 vTmp = new Vector2();
    private Vector2 vTmp2 = new Vector2();
    private float lastPuffX;
    private float lastPuffY;
    private DialogRenderSystem dialogRenderSystem;
    private float jetPackMessageCooldown;

    public PlayerControlSystem() {
        super(Aspect.getAspectForAll(PlayerControlled.class, Physics.class, WallSensor.class, Anim.class));
    }

    Vector2 tmp = new Vector2();

    float jetPackUsageTime = 0;
    float jetPackRollingTime = 0;

    @Override
    protected void process(Entity player) {

        jetPackMessageCooldown -= world.delta;
        final Physics physics = ym.get(player);
        final Gravity gravity = gm.get(player);

        // frozen player does not act.
        if (fm.has(player)) {
            stopJetpackLooping();
            player.getComponent(Anim.class).id = "player-respawning";
            gravity.enabled = false;
            physics.vr = physics.vx = physics.vy = 0;
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isButtonPressed(0)) {
            Inventory inventory = im.get(player);
            if (inventory.weapon != null && inventory.weapon.isActive()) {
                Weapon weapon = wem.get(inventory.weapon);
                weapon.firing = true;
            }
        }

        final WallSensor wallSensor = wm.get(player);
        final Pos pos = pm.get(player);
        final Anim anim = am.get(player);

        gravity.enabled = true;
        if (wallSensor.onFloor) {
            stopJetpackLooping();

            anim.id = "player-idle";

            // handle player walking. move left and right, rotation always 0.
            anim.rotation = 0;
            physics.vr = 0;

            float dx = 0;
            float dy = 0;

            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                dx = -MOVEMENT_FACTOR;
                flip(player, true);

            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                dx = MOVEMENT_FACTOR;
                flip(player, false);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.W)) // jump
            {
                dy = JUMP_FACTOR;
            }
            ;

            if (dx != 0) {
                physics.vx += dx * world.delta;
                anim.id = "player-walk";
            }
            if (dy != 0) physics.vy += dy * world.delta;
        } else {


            anim.id = "player-jetpack";
            // handle player flying! :D

            boolean jetPackActive = Gdx.input.isKeyPressed(Input.Keys.W);

            float turningSpeed = jetPackActive ? ROTATIONAL_SPEED_JETPACK_ON : ROTATIONAL_SPEED_JETPACK_OFF;

            // slow turning.
            int rx = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) rx += turningSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) rx -= turningSpeed;

            if (rx != 0) {
                if (!jetPackActive) {
                    jetPackRollingTime += world.delta;
                    if (jetPackRollingTime >= 1 && jetPackMessageCooldown <= 0) {
                        jetPackMessageCooldown = 10;
                        dialogRenderSystem.randomSay(DialogRenderSystem.JETPACK_ROLLING);
                    }
                } else jetPackRollingTime=0;
                physics.vr += rx * world.delta * 10f;
            } else {
                jetPackRollingTime = 0;

                // not steering, not jetpack active? auto-straighten
                if (!jetPackActive) {
                    float rotationClamped = ((anim.rotation % 360 + 360) % 360);

                    if (rotationClamped < 180) physics.vr -= turningSpeed * world.delta * 1f;
                    if (rotationClamped > 180) physics.vr += turningSpeed * world.delta * 1f;
                    if (rotationClamped < 90) physics.vr -= turningSpeed * world.delta * 1f;
                    if (rotationClamped > 180 + 90) physics.vr += turningSpeed * world.delta * 1f;
                }
            }

            if (jetPackActive) {
                startJetpackLooping();
                jetpackGasCooldown -= world.delta;

                float distanceMoved = vTmp.set(lastPuffX, lastPuffY).dst(pos.x, pos.y);

                if ( distanceMoved <= 0.1f )
                    jetPackUsageTime=0;

                if (jetpackGasCooldown <= 0 || distanceMoved >= 4f) {
                    lastPuffX = pos.x;
                    lastPuffY = pos.y;
                    jetpackGasCooldown = 1 / 15f;

                    vTmp.set(anim.flippedX ? 8 : -8, 0).rotate(anim.rotation).add(pos.x + 16, pos.y + 16);
                    particleSystem.setRotation(anim.rotation - (10f + MathUtils.random(-10f, 10f) * (anim.flippedX ? -1f : 1f)));
                    particleSystem.spawnParticle((int) (vTmp.x), (int) (vTmp.y), "puff");

                    particleSystem.setRotation(anim.rotation - 10f * (anim.flippedX ? -1f : 1f));
                    vTmp.set(anim.flippedX ? 8 : -8, -13).rotate(anim.rotation).add(pos.x + 16, pos.y + 16);
                    particleSystem.spawnParticle((int) (vTmp.x), (int) (vTmp.y), "gasburn");
                    particleSystem.setRotation(0);
                }
                gravity.enabled = false;
                physicsSystem.push(player, anim.rotation + THRUST_VECTOR, JETPACK_THRUST * world.delta);
                physicsSystem.clampVelocity(player, 0, 100);
                physics.vr = MathUtils.clamp(physics.vr, -ROTATIONAL_SPEED_JETPACK_ON * 2, ROTATIONAL_SPEED_JETPACK_ON * 2); // clamp our rotation while accelerating.
            } else {
                stopJetpackLooping();
                gravity.enabled = true;
                physics.vr = MathUtils.clamp(physics.vr, -ROTATIONAL_SPEED_JETPACK_OFF * 2, ROTATIONAL_SPEED_JETPACK_OFF * 2);
            }
        }


    }

    private void startJetpackLooping() {
        jetPackUsageTime += world.delta;
        if (jetPackUsageTime > 2 && jetPackMessageCooldown <= 0) {
            jetPackMessageCooldown = 20;
            dialogRenderSystem.randomSay(DialogRenderSystem.JETPACK_USAGE);
        }

        if (!jetpackLooping) {
            jetpackLooping = true;
            assetSystem.getSfx("ns2d_sfx_jetpack_loop").loop(0.09f);
            assetSystem.getSfx("ns2d_sfx_jetpack_start").play(0.05f);
        }
    }

    private void stopJetpackLooping() {
        jetPackUsageTime = 0;
        if (jetpackLooping) {
            jetpackLooping = false;
            assetSystem.getSfx("ns2d_sfx_jetpack_loop").stop();
        }
    }

    private void flip(Entity player, boolean flippedX) {
        final Anim anim = am.get(player);
        anim.flippedX = flippedX;

/*        Inventory inventory = im.get(player);
        if ( inventory.weapon != null && inventory.weapon.isActive() )
        {
            Attached weaponAttached = attm.get(inventory.weapon);
            Anim weaponAnim = am.get(inventory.weapon);
            weaponAnim.flippedX = flippedX;

        } */
    }
}
