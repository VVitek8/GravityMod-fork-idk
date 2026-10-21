package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;






@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.PortalEntity", remap = false)
public interface PortalModPortalEntityAccessorMixin {

    @Accessor("direction")
    Direction gravitymod$getPortalDirection();
}
