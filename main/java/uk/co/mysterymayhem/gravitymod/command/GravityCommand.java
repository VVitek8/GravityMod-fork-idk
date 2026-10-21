package uk.co.mysterymayhem.gravitymod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;














public class GravityCommand {

    private static final DynamicCommandExceptionType INVALID_DIRECTION = new DynamicCommandExceptionType(
            arg -> new StringTextComponent("Unknown gravity direction: " + arg));

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("gravity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(GravityCommand::executeGet)))
                .then(Commands.literal("set")
                        .then(Commands.argument("direction", StringArgumentType.word())
                                .suggests(GravityCommand::suggestDirections)
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(context -> executeSet(context, false)))))
                .then(Commands.literal("rotate")
                        .then(Commands.argument("direction", StringArgumentType.word())
                                .suggests(GravityCommand::suggestDirections)
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(GravityCommand::executeRotate))))
                .then(Commands.literal("strength")
                        .then(Commands.argument("strength", DoubleArgumentType.doubleArg(0.0D, 10.0D))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(GravityCommand::executeStrength)))));
    }

    private static CompletableFuture<Suggestions> suggestDirections(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        for (EnumGravityDirection direction : EnumGravityDirection.values()) {
            builder.suggest(direction.getSerializedName());
        }
        return builder.buildFuture();
    }

    private static int executeGet(CommandContext<CommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        EnumGravityDirection direction = GravityAPI.getDirection(target);
        int index = direction.ordinal();
        context.getSource().sendSuccess(
                new StringTextComponent(target.getDisplayName().getString() + "'s gravity direction is "
                        + direction.getSerializedName() + " (" + index + ")"),
                false);
        return index;
    }

    private static int executeSet(CommandContext<CommandSource> context, boolean unused) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        EnumGravityDirection direction = parseDirection(StringArgumentType.getString(context, "direction"));
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        int affected = 0;
        for (Entity target : targets) {
            if (!hasGravity(target)) {
                continue;
            }
            GravityAPI.setDirection(target, direction, true);
            affected++;
        }
        int count = affected;
        context.getSource().sendSuccess(
                new StringTextComponent("Set gravity direction of " + count + " entity/entities to " + direction.getSerializedName()
                        + skippedSuffix(targets.size() - count)),
                true);
        return count;
    }

    





    private static int executeRotate(CommandContext<CommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        EnumGravityDirection relativeDirection = parseDirection(StringArgumentType.getString(context, "direction"));
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        int affected = 0;
        for (Entity target : targets) {
            if (!hasGravity(target)) {
                continue;
            }
            EnumGravityDirection lookBasis = directionFromLookVec(target.getLookAngle());
            EnumGravityDirection newDirection = rotateRelativeTo(lookBasis, relativeDirection);
            GravityAPI.setDirection(target, newDirection, true);
            affected++;
        }
        int count = affected;
        context.getSource().sendSuccess(
                new StringTextComponent("Rotated gravity direction of " + count + " entity/entities by " + relativeDirection.getSerializedName()
                        + skippedSuffix(targets.size() - count)),
                true);
        return count;
    }

    



    private static int executeStrength(CommandContext<CommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        double strength = DoubleArgumentType.getDouble(context, "strength");
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        int affected = 0;
        for (Entity target : targets) {
            if (!hasGravity(target)) {
                continue;
            }
            GravityAPI.setStrength(target, strength);
            affected++;
        }
        int count = affected;
        context.getSource().sendSuccess(
                new StringTextComponent("Set gravity strength of " + count + " entity/entities to " + strength + "x"
                        + skippedSuffix(targets.size() - count)),
                true);
        return count;
    }

    




    private static boolean hasGravity(Entity entity) {
        return GravityAPI.getCapability(entity).isPresent();
    }

    private static String skippedSuffix(int skipped) {
        return skipped > 0 ? " (skipped " + skipped + " entity/entities that cannot have gravity)" : "";
    }

    




    private static EnumGravityDirection rotateRelativeTo(EnumGravityDirection lookBasis, EnumGravityDirection relative) {
        
        
        double[] worldVec = lookBasis.adjustXYZValues(
                relative == EnumGravityDirection.WEST ? -1 : relative == EnumGravityDirection.EAST ? 1 : 0,
                relative == EnumGravityDirection.DOWN ? -1 : relative == EnumGravityDirection.UP ? 1 : 0,
                relative == EnumGravityDirection.NORTH ? -1 : relative == EnumGravityDirection.SOUTH ? 1 : 0
        );
        return directionFromLookVec(new Vector3d(worldVec[0], worldVec[1], worldVec[2]));
    }

    private static EnumGravityDirection directionFromLookVec(Vector3d look) {
        double absX = Math.abs(look.x);
        double absY = Math.abs(look.y);
        double absZ = Math.abs(look.z);
        if (absY >= absX && absY >= absZ) {
            return look.y > 0 ? EnumGravityDirection.UP : EnumGravityDirection.DOWN;
        }
        else if (absX >= absZ) {
            return look.x > 0 ? EnumGravityDirection.EAST : EnumGravityDirection.WEST;
        }
        else {
            return look.z > 0 ? EnumGravityDirection.SOUTH : EnumGravityDirection.NORTH;
        }
    }

    private static EnumGravityDirection parseDirection(String input) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try {
            int ordinal = Integer.parseInt(input);
            EnumGravityDirection[] values = EnumGravityDirection.values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
            throw INVALID_DIRECTION.create(input);
        }
        catch (NumberFormatException ignored) {
            for (EnumGravityDirection direction : EnumGravityDirection.values()) {
                if (direction.getSerializedName().equalsIgnoreCase(input)) {
                    return direction;
                }
            }
            throw INVALID_DIRECTION.create(input);
        }
    }
}
