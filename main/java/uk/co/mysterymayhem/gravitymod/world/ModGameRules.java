package uk.co.mysterymayhem.gravitymod.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import uk.co.mysterymayhem.gravitymod.mixin.accessor.GameRuleBooleanValueInvoker;
import uk.co.mysterymayhem.gravitymod.network.CubePhysicsRulePacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;




















public final class ModGameRules {

    private ModGameRules() {}

    




    private static GameRules.RuleKey<GameRules.BooleanValue> cubePhysics;

    




    private static volatile boolean clientCubePhysics = true;

    







    public static void register() {
        if (cubePhysics != null) {
            return;
        }
        cubePhysics = GameRules.register("cubePhysics", GameRules.Category.MISC,
                GameRuleBooleanValueInvoker.gravitymod$create(true, ModGameRules::onCubePhysicsChanged));
    }

    





    public static boolean isCubePhysicsEnabled(World level) {
        if (level == null) {
            return true;
        }
        if (level.isClientSide) {
            return clientCubePhysics;
        }
        if (cubePhysics == null) {
            return true;
        }
        return level.getGameRules().getBoolean(cubePhysics);
    }

    
    public static void acceptClientValue(boolean enabled) {
        clientCubePhysics = enabled;
    }

    




    public static boolean isCubePhysicsEnabled(MinecraftServer server) {
        if (server == null || cubePhysics == null) {
            return true;
        }
        return server.getGameRules().getBoolean(cubePhysics);
    }

    



    private static void onCubePhysicsChanged(MinecraftServer server, GameRules.BooleanValue value) {
        if (server == null) {
            return;
        }
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CubePhysicsRulePacket(value.get()));
    }
}
