package uk.co.mysterymayhem.gravitymod.capability;

import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import javax.annotation.Nonnull;

public interface IGravityCapability {

    int DEFAULT_TIMEOUT_TICKS = 10;
    double DEFAULT_STRENGTH = 1.0D;
    double MIN_STRENGTH = 0.0D;
    double MAX_STRENGTH = 10.0D;

    




    int TRANSITION_TICKS = 8;

    @Nonnull
    EnumGravityDirection getDirection();

    void setDirection(@Nonnull EnumGravityDirection direction, boolean noTimeout);

    




    double getStrength();

    void setStrength(double strength);

    int getTimeoutTicks();

    void setTimeoutTicks(int ticks);

    








    void beginTransition(@Nonnull EnumGravityDirection from);

    


    int getTransitionTicks();

    



    @Nonnull
    EnumGravityDirection getTransitionFrom();

    void tick();
}
