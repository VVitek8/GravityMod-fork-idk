package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Quaternion;
import net.portalmod.core.math.Mat4;





























public final class PortalModCameraHelper {

    private PortalModCameraHelper() {}

    


















    public static Mat4 inViewerFrame(Mat4 rotation, Entity viewer, float partialTicks) {
        if (viewer == null || GravityRenderHelper.isIdentity(viewer, partialTicks)) {
            return rotation;
        }
        Quaternion inverse = GravityRenderHelper.getInverseRotation(viewer, partialTicks);
        Quaternion forward = GravityRenderHelper.getRotation(viewer, partialTicks);
        
        
        return new Mat4(inverse).mul(rotation).mul(new Mat4(forward));
    }

    








    public static Quaternion unroll(ActiveRenderInfo camera, float partialTicks) {
        Entity viewer = camera == null ? null : camera.getEntity();
        if (viewer == null || GravityRenderHelper.isIdentity(viewer, partialTicks)) {
            return null;
        }
        return GravityRenderHelper.getInverseRotation(viewer, partialTicks);
    }
}
