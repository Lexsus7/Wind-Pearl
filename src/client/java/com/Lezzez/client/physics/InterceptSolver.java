package com.Lezzez.client.physics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
public final class InterceptSolver {
    public record Result(float yaw,float pitch) {}
    private static final double WIND_SPEED = 1.5;
    private static double equation(Vec3 start,Vec3 p,Vec3 v,Vec3 inherited,double delay,double t) {
        return PearlPhysics.predict(p,v,delay+t).subtract(start).subtract(inherited.scale(t)).length()-WIND_SPEED*t;
    }
    public static Result solve(Vec3 start,Vec3 p,Vec3 v,Vec3 inherited,double delay,boolean vertical,Minecraft client) {
        double previous=0, f=equation(start,p,v,inherited,delay,0);
        for(int i=1;i<=400;i++) {
            double t=i*0.2, next=equation(start,p,v,inherited,delay,t);
            if(f*next<=0) {
                double lo=previous,hi=t, left=f;
                for(int j=0;j<36;j++) {
                    double mid=(lo+hi)*0.5, value=equation(start,p,v,inherited,delay,mid);
                    if(left*value<=0) hi=mid; else {lo=mid;left=value;}
                }
                double root=(lo+hi)*0.5;
                Vec3 target=PearlPhysics.predict(p,v,delay+root);
                double distance=start.distanceTo(target);
                if(root>0.01 && distance >= (vertical?1:20) && distance<=40) {
                    Vec3 aim=target.subtract(start).subtract(inherited.scale(root));
                    if(aim.length()>1e-8 && PathValidator.valid(client,start,p,v,delay+root,target)) {
                        Vec3 direction=aim.normalize();
                        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)));
                        return new Result(yaw,pitch);
                    }
                }
            }
            previous=t; f=next;
        }
        return null;
    }
}
