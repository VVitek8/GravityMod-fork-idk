package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import javax.annotation.Nullable;


















public final class GravityPlacementHelper {

    private GravityPlacementHelper() {
    }

    




    @Nullable
    public static Boolean isWorldLowerHalf(BlockItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return null;
        }
        EnumGravityDirection gravity = GravityPhysicsHelper.getDirection(player);
        if (gravity == EnumGravityDirection.DOWN) {
            return null;
        }
        Direction relativeUp = gravity.getRelativeUp().getFacingEquivalent();
        if (relativeUp.getAxis().isHorizontal()) {
            
            return null;
        }

        boolean relativeLower = isRelativeLowerHalf(context, relativeUp);
        
        
        return !relativeLower;
    }

    




    private static boolean isRelativeLowerHalf(BlockItemUseContext context, Direction relativeUp) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == relativeUp) {
            return true;
        }
        if (clickedFace == relativeUp.getOpposite()) {
            return false;
        }
        BlockPos pos = context.getClickedPos();
        Vector3d hit = context.getClickLocation();
        double withinBlock;
        switch (relativeUp.getAxis()) {
            case X:
                withinBlock = hit.x - pos.getX();
                break;
            case Y:
                withinBlock = hit.y - pos.getY();
                break;
            default:
                withinBlock = hit.z - pos.getZ();
                break;
        }
        if (relativeUp.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            withinBlock = 1.0D - withinBlock;
        }
        return withinBlock <= 0.5D;
    }
}
