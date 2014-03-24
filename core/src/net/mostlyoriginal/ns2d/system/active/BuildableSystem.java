package net.mostlyoriginal.ns2d.system.active;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import net.mostlyoriginal.ns2d.component.*;
import net.mostlyoriginal.ns2d.system.passive.CollisionSystem;

/**
 * @author Daan van Yperen
 */
@Wire
public class BuildableSystem extends EntityProcessingSystem {

    ComponentMapper<Buildable> bm;
    ComponentMapper<Pos> pm;
    ComponentMapper<Anim> am;
    ComponentMapper<Wallet> wm;
    ComponentMapper<Bounds> om;

    ParticleSystem particleSystem;
    CollisionSystem collisionSystem;

    TagManager tagManager;
    public Entity player;

    public BuildableSystem()
    {
        super(Aspect.getAspectForAll(Buildable.class,Pos.class,Bounds.class));
    }

    @Override
    protected void begin() {
        player = tagManager.getEntity("player");
    }

    @Override
    protected void process(Entity e) {
        final Buildable buildable = bm.get(e);
        if ( !buildable.built && collisionSystem.overlaps(player, e))
        {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.E))
            {
                Wallet wallet = wm.get(player);
                if (wallet.resources >= buildable.resourceCost)
                {
                    wallet.resources -= buildable.resourceCost;
                }
                buildable.built = true;
                Anim anim = am.get(e);
                anim.id = buildable.builtAnimId;
                e.addComponent(new Health(2)).changedInWorld();
            }
        }
    }

    Vector2 vTmp = new Vector2();

    public void destroyBuildable(Entity victim) {
        final Buildable buildable = bm.get(victim);
        if ( buildable.built )
        {
            Pos pos = pm.get(victim);
            Bounds bounds = om.get(victim);

            particleSystem.spawnParticle( (int)(pos.x + bounds.cx()), (int)(pos.y + bounds.cy()), "explosion" );
            for ( int i=0, s= MathUtils.random(3, 5); i<s; i++ ) {
                vTmp.set(MathUtils.random(0,50),0).rotate(MathUtils.random(0,360)).add(pos.x,pos.y);
                particleSystem.spawnParticle(
                        (int)vTmp.x, (int)vTmp.y, "tiny-explosion");
            }

            buildable.built = false;
            Anim anim = am.get(victim);
            anim.id = buildable.unbuiltAnimId;
        }
    }
}
