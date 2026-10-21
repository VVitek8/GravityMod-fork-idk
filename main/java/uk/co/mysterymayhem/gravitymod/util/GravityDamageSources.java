package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;







public final class GravityDamageSources {

    private GravityDamageSources() {}

    





    public static final DamageSource LACK_OF_OXYGEN = new DamageSource("gravitymod.lack_of_oxygen") {
        @Override
        public ITextComponent getLocalizedDeathMessage(LivingEntity entity) {
            return new StringTextComponent("")
                    .append(entity.getDisplayName())
                    .append(" died from a lack of oxygen");
        }
    }.bypassArmor();
}
