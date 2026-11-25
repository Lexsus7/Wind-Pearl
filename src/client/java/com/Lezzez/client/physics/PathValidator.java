package com.Lezzez.client.physics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
public final class PathValidator {
    private static boolean clear(Minecraft client, Vec3 a, Vec3 b) {
        int count = Math.max(1, (int)Math.ceil(a.distanceTo(b) / 0.25));
        for (int i=0;i<=count;i++) {
            Vec3 p = a.add(b.subtract(a).scale(i/(double)count));
            if (!client.level.getFluidState(BlockPos.containing(p.x,p.y,p.z)).isEmpty()) return false;
        }
        return client.level.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,client.player)).getType() != HitResult.Type.BLOCK;
    }
    public static boolean valid(Minecraft client, Vec3 start, Vec3 p, Vec3 v, double age, Vec3 target) {
        if (client.level == null || client.player == null) return false;
        Vec3 previous=p;
        for(double t=0;t<age;) {
            t=Math.min(age,t+0.25);
            Vec3 next=PearlPhysics.predict(p,v,t);
            if(!clear(client,previous,next)) return false;
            previous=next;
        }
        return clear(client,start,target);
    }
}
