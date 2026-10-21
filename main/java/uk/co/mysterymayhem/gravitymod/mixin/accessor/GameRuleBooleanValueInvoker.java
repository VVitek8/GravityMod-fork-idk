package uk.co.mysterymayhem.gravitymod.mixin.accessor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiConsumer;














@Mixin(GameRules.BooleanValue.class)
public interface GameRuleBooleanValueInvoker {

    @Invoker("create")
    static GameRules.RuleType<GameRules.BooleanValue> gravitymod$create(
            boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> onChanged) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
