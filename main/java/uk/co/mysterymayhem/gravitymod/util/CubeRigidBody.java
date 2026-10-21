package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;












































public final class CubeRigidBody {

    



    private static final double MASS = 1.0D;

    
    private static final double SUBSTEP_TRAVEL = 0.15D;

    




    private static final int MAX_SUBSTEPS = 12;

    
    private static final int VELOCITY_ITERATIONS = 8;

    
    private static final int POSITION_ITERATIONS = 4;

    
    private static final int RESTITUTION_ITERATIONS = 4;

    





    private static final double CONTACT_SKIN = 0.008D;

    
    private static final double PENETRATION_SLOP = 0.0002D;

    












    private static final double REST_GAP = 0.002D;

    



    private static final double REST_MAX_PENETRATION = 0.01D;

    
    private static final double POSITION_BIAS = 0.6D;

    




    private static final int PUSH_OUT_PASSES = 3;

    




    private static final double MAX_CORRECTION = 0.2D;

    



    private static final double BASE_TICK_GRAVITY = 0.08D;

    












    private static final double RESTITUTION_THRESHOLD = 0.25D;

    
    private static final double RESTITUTION_THRESHOLD_TICKS = RESTITUTION_THRESHOLD / BASE_TICK_GRAVITY;

    
    private static final int MAX_CONTACTS = 32;

    







    private static final double EDGE_AXIS_BIAS = 0.002D;

    
    private static final double REST_LINEAR_SPEED = 0.005D;

    private static final double REST_ANGULAR_SPEED = 0.02D;

    
    private static final int REST_TICKS = 6;

    
    private static final double MAX_ANGULAR_SPEED = 1.5D;

    
    private static final double ANGULAR_DRAG = 0.995D;

    







    private static final double SCATTER_THRESHOLD = 0.35D;

    
    private static final double SCATTER_THRESHOLD_TICKS = SCATTER_THRESHOLD / BASE_TICK_GRAVITY;

    







    private static final double SCATTER_GAIN = 0.22D;

    





    private static final double SCATTER_MAX = 0.55D;

    






    private static final double SCATTER_TUMBLE = 0.55D;

    
    private static final double SCATTER_SLIDE_EPSILON = 1.0E-4D;

    
    private static final double SUPPORT_DOT = 0.35D;

    
    private static final double WAKE_SPEED = 0.01D;

    





    private static final double WAKE_FALL_SPEED = 0.2D;

    
    private static final double WAKE_FALL_SPEED_TICKS = WAKE_FALL_SPEED / BASE_TICK_GRAVITY;

    
    private static final double SYNC_EPSILON = 1.0E-4D;

    



    private static final int PLAYOUT_CAPACITY = 4;

    
    private static final int PLAYOUT_CATCHUP = 3;

    
    
    

    
    private double qx, qy, qz, qw = 1.0D;

    
    private double px, py, pz, pw = 1.0D;

    
    private Vector3d axisX = new Vector3d(1.0D, 0.0D, 0.0D);
    private Vector3d axisY = new Vector3d(0.0D, 1.0D, 0.0D);
    private Vector3d axisZ = new Vector3d(0.0D, 0.0D, 1.0D);

    private Vector3d angularVelocity = Vector3d.ZERO;

    






    private final Random random = new Random();

    




    private Vector3d pendingSpin = Vector3d.ZERO;

    



    private Vector3d linearVelocity = Vector3d.ZERO;

    








    private Vector3d pendingImpulse = Vector3d.ZERO;

    
    private Vector3d solvedVelocity;

    private boolean grounded;

    




    private boolean controlling;

    private boolean resting;

    private int restTicks;

    
    private EnumGravityDirection lastDirection;

    



    private double tickGravity = BASE_TICK_GRAVITY;

    
    private double halfX = 0.40625D, halfY = 0.40625D, halfZ = 0.40625D;

    private double invIx = 1.0D, invIy = 1.0D, invIz = 1.0D;

    



    private ArrayDeque<Sample> playout;

    
    private boolean primed;

    
    private boolean snapNext;

    
    private double sentX, sentY, sentZ, sentW = 1.0D;

    private boolean sentControlling;

    private boolean everSent;

    
    
    

    public boolean isControlling() {
        return this.controlling;
    }

    public boolean isResting() {
        return this.resting;
    }

    



    boolean isBraced() {
        return this.resting || this.grounded;
    }

    public Vector3d getAngularVelocity() {
        return this.angularVelocity;
    }

    public Vector3d axisX() {
        return this.axisX;
    }

    public Vector3d axisY() {
        return this.axisY;
    }

    public Vector3d axisZ() {
        return this.axisZ;
    }

    






    public void addSpin(Vector3d spin) {
        if (!this.controlling) {
            
            
            
            
            this.pendingSpin = clampSpeed(this.pendingSpin.add(spin), MAX_ANGULAR_SPEED);
            return;
        }
        this.angularVelocity = clampSpeed(this.angularVelocity.add(spin), MAX_ANGULAR_SPEED);
        wake();
    }

    
    public void wake() {
        this.resting = false;
        this.restTicks = 0;
    }

    








    void disturb() {
        this.resting = false;
    }

    







    public void setOrientation(double x, double y, double z, double w) {
        double norm = Math.sqrt(x * x + y * y + z * z + w * w);
        if (norm < 1.0E-12D) {
            x = 0.0D;
            y = 0.0D;
            z = 0.0D;
            w = 1.0D;
            norm = 1.0D;
        }
        this.qx = x / norm;
        this.qy = y / norm;
        this.qz = z / norm;
        this.qw = w / norm;
        refreshAxes();
    }

    
    public void snapshotOrientation() {
        this.px = this.qx;
        this.py = this.qy;
        this.pz = this.qz;
        this.pw = this.qw;
    }

    private void refreshAxes() {
        double x = this.qx, y = this.qy, z = this.qz, w = this.qw;
        this.axisX = new Vector3d(1.0D - 2.0D * (y * y + z * z), 2.0D * (x * y + z * w), 2.0D * (x * z - y * w));
        this.axisY = new Vector3d(2.0D * (x * y - z * w), 1.0D - 2.0D * (x * x + z * z), 2.0D * (y * z + x * w));
        this.axisZ = new Vector3d(2.0D * (x * z + y * w), 2.0D * (y * z - x * w), 1.0D - 2.0D * (x * x + y * y));
    }

    






    private void integrateRotation(Vector3d omega, double dt) {
        if (omega.lengthSqr() == 0.0D || dt == 0.0D) {
            return;
        }
        double half = 0.5D * dt;
        double dx = half * (omega.x * this.qw + omega.y * this.qz - omega.z * this.qy);
        double dy = half * (omega.y * this.qw + omega.z * this.qx - omega.x * this.qz);
        double dz = half * (omega.z * this.qw + omega.x * this.qy - omega.y * this.qx);
        double dw = -half * (omega.x * this.qx + omega.y * this.qy + omega.z * this.qz);
        setOrientation(this.qx + dx, this.qy + dy, this.qz + dz, this.qw + dw);
    }

    








    public Quaternion renderOrientation(float partialTicks) {
        if (partialTicks >= 1.0F) {
            return new Quaternion((float) this.qx, (float) this.qy, (float) this.qz, (float) this.qw);
        }
        double ax = this.px, ay = this.py, az = this.pz, aw = this.pw;
        double dot = ax * this.qx + ay * this.qy + az * this.qz + aw * this.qw;
        if (dot < 0.0D) {
            
            
            ax = -ax;
            ay = -ay;
            az = -az;
            aw = -aw;
            dot = -dot;
        }
        double weightA;
        double weightB;
        if (dot > 0.9995D) {
            weightA = 1.0D - partialTicks;
            weightB = partialTicks;
        }
        else {
            double theta = Math.acos(dot);
            double sinTheta = Math.sin(theta);
            weightA = Math.sin((1.0D - partialTicks) * theta) / sinTheta;
            weightB = Math.sin(partialTicks * theta) / sinTheta;
        }
        double rx = ax * weightA + this.qx * weightB;
        double ry = ay * weightA + this.qy * weightB;
        double rz = az * weightA + this.qz * weightB;
        double rw = aw * weightA + this.qw * weightB;
        double norm = Math.sqrt(rx * rx + ry * ry + rz * rz + rw * rw);
        if (norm < 1.0E-12D) {
            return new Quaternion((float) this.qx, (float) this.qy, (float) this.qz, (float) this.qw);
        }
        return new Quaternion((float) (rx / norm), (float) (ry / norm), (float) (rz / norm), (float) (rw / norm));
    }

    
    
    

    







    void beginControl(Entity cube, EnumGravityDirection direction) {
        if (!this.controlling) {
            adoptFacing(cube, direction);
            this.controlling = true;
            
            
            this.angularVelocity = this.pendingSpin;
            this.pendingSpin = Vector3d.ZERO;
            wake();
        }
        snapshotOrientation();
    }

    
    void releaseControl(Entity cube, EnumGravityDirection direction) {
        if (this.controlling) {
            this.controlling = false;
            this.angularVelocity = Vector3d.ZERO;
            this.pendingSpin = Vector3d.ZERO;
            this.linearVelocity = Vector3d.ZERO;
            this.pendingImpulse = Vector3d.ZERO;
            this.solvedVelocity = null;
            wake();
        }
        adoptFacing(cube, direction);
        snapshotOrientation();
    }

    
    private void adoptFacing(Entity cube, EnumGravityDirection direction) {
        float facing = cube instanceof LivingEntity ? ((LivingEntity) cube).yBodyRot : cube.yRot;
        double radians = Math.toRadians(facing);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vector3d x = direction.adjustVec(new Vector3d(cos, 0.0D, sin));
        Vector3d y = direction.adjustVec(new Vector3d(0.0D, 1.0D, 0.0D));
        Vector3d z = direction.adjustVec(new Vector3d(-sin, 0.0D, cos));
        setFromAxes(x, y, z);
    }

    
    private void setFromAxes(Vector3d x, Vector3d y, Vector3d z) {
        double m00 = x.x, m10 = x.y, m20 = x.z;
        double m01 = y.x, m11 = y.y, m21 = y.z;
        double m02 = z.x, m12 = z.y, m22 = z.z;
        double trace = m00 + m11 + m22;
        if (trace > 0.0D) {
            double s = Math.sqrt(trace + 1.0D) * 2.0D;
            setOrientation((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25D * s);
        }
        else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1.0D + m00 - m11 - m22) * 2.0D;
            setOrientation(0.25D * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s);
        }
        else if (m11 > m22) {
            double s = Math.sqrt(1.0D + m11 - m00 - m22) * 2.0D;
            setOrientation((m01 + m10) / s, 0.25D * s, (m12 + m21) / s, (m02 - m20) / s);
        }
        else {
            double s = Math.sqrt(1.0D + m22 - m00 - m11) * 2.0D;
            setOrientation((m02 + m20) / s, (m12 + m21) / s, 0.25D * s, (m10 - m01) / s);
        }
    }

    







    void configure(Entity cube) {
        this.halfX = CubeModelCollisionHelper.modelHalfSide(cube);
        this.halfZ = this.halfX;
        this.halfY = CubeModelCollisionHelper.modelHalfHeight(cube);
        this.invIx = 3.0D / (MASS * (this.halfY * this.halfY + this.halfZ * this.halfZ));
        this.invIy = 3.0D / (MASS * (this.halfX * this.halfX + this.halfZ * this.halfZ));
        this.invIz = 3.0D / (MASS * (this.halfX * this.halfX + this.halfY * this.halfY));
    }

    
    private Vector3d applyInverseInertia(Vector3d v) {
        return this.axisX.scale(this.axisX.dot(v) * this.invIx)
                .add(this.axisY.scale(this.axisY.dot(v) * this.invIy))
                .add(this.axisZ.scale(this.axisZ.dot(v) * this.invIz));
    }

    
    private Vector3d applyInertia(Vector3d v) {
        return this.axisX.scale(this.axisX.dot(v) / this.invIx)
                .add(this.axisY.scale(this.axisY.dot(v) / this.invIy))
                .add(this.axisZ.scale(this.axisZ.dot(v) / this.invIz));
    }

    











    private Vector3d gyroscopicDelta(double dt) {
        if (this.angularVelocity.lengthSqr() == 0.0D) {
            return Vector3d.ZERO;
        }
        return applyInverseInertia(this.angularVelocity.cross(applyInertia(this.angularVelocity))).scale(-dt);
    }

    
    private double effectiveMass(Vector3d r, Vector3d direction) {
        Vector3d angular = applyInverseInertia(r.cross(direction)).cross(r);
        return 1.0D / MASS + direction.dot(angular);
    }

    




    double effectiveMassAt(Vector3d r, Vector3d direction) {
        return effectiveMass(r, direction);
    }

    
    private Vector3d pointVelocity(Vector3d r) {
        return this.linearVelocity.add(this.angularVelocity.cross(r));
    }

    

















    private double impactThreshold(double ticks) {
        return ticks * Math.max(BASE_TICK_GRAVITY, this.tickGravity);
    }

    






    Vector3d step(Entity cube, Vector3d com, Vector3d movement, EnumGravityDirection direction, double tickGravity,
                  List<CubePhysics.Obstacle> obstacles) {
        Vector3d up = direction.adjustVec(new Vector3d(0.0D, 1.0D, 0.0D));
        this.tickGravity = tickGravity;
        this.linearVelocity = sanitise(movement).add(this.pendingImpulse);
        this.pendingImpulse = Vector3d.ZERO;

        if (direction != this.lastDirection) {
            
            this.lastDirection = direction;
            wake();
        }
        if (this.resting && isDisturbed(direction, this.linearVelocity, impactThreshold(WAKE_FALL_SPEED_TICKS))) {
            wake();
        }

        if (this.resting) {
            
            
            List<Contact> contacts = collectContacts(cube, com, obstacles, 1.0D);
            
            
            if (isSupported(contacts, up) && deepest(contacts) < REST_MAX_PENETRATION) {
                this.linearVelocity = Vector3d.ZERO;
                this.solvedVelocity = Vector3d.ZERO;
                this.grounded = true;
                return com;
            }
            wake();
        }

        double radius = Math.sqrt(this.halfX * this.halfX + this.halfY * this.halfY + this.halfZ * this.halfZ);
        double travel = Math.max(this.linearVelocity.length(), this.angularVelocity.length() * radius);
        int substeps = MathHelper.clamp((int) Math.ceil(travel / SUBSTEP_TRAVEL), 1, MAX_SUBSTEPS);
        double dt = 1.0D / substeps;

        boolean supported = false;
        double penetration = 0.0D;
        for (int substep = 0; substep < substeps; substep++) {
            List<Contact> contacts = collectContacts(cube, com, obstacles, dt);
            supported |= isSupported(contacts, up);
            solveVelocities(contacts, dt);
            com = com.add(this.linearVelocity.scale(dt));
            this.angularVelocity = this.angularVelocity.add(gyroscopicDelta(dt));
            integrateRotation(this.angularVelocity, dt);
            
            
            applyRestitution(contacts);
            
            
            applyImpactScatter(contacts);

            
            
            
            
            
            
            for (int pass = 0; pass < PUSH_OUT_PASSES; pass++) {
                List<Contact> settled = collectContacts(cube, com, obstacles, dt);
                penetration = deepest(settled);
                if (penetration <= PENETRATION_SLOP) {
                    break;
                }
                prepareContacts(settled, dt);
                com = com.add(solvePositions(settled, dt));
            }
        }

        this.angularVelocity = clampSpeed(this.angularVelocity.scale(ANGULAR_DRAG), MAX_ANGULAR_SPEED);
        this.grounded = supported;
        this.solvedVelocity = this.linearVelocity;

        if (supported
                && penetration < REST_MAX_PENETRATION
                && this.linearVelocity.lengthSqr() < REST_LINEAR_SPEED * REST_LINEAR_SPEED
                && this.angularVelocity.lengthSqr() < REST_ANGULAR_SPEED * REST_ANGULAR_SPEED) {
            if (++this.restTicks >= REST_TICKS) {
                this.resting = true;
                this.angularVelocity = Vector3d.ZERO;
                this.linearVelocity = Vector3d.ZERO;
                this.solvedVelocity = Vector3d.ZERO;
            }
        }
        else {
            this.restTicks = 0;
        }
        return com;
    }

    








    private static boolean isDisturbed(EnumGravityDirection direction, Vector3d velocity, double wakeFallSpeed) {
        Vector3d relative = direction.inverseAdjustVec(velocity);
        return relative.x * relative.x + relative.z * relative.z > WAKE_SPEED * WAKE_SPEED
                || relative.y > WAKE_SPEED
                || relative.y < -wakeFallSpeed;
    }

    
    private static double deepest(List<Contact> contacts) {
        double deepest = 0.0D;
        for (int i = 0; i < contacts.size(); i++) {
            deepest = Math.max(deepest, contacts.get(i).depth);
        }
        return deepest;
    }

    
    private boolean isSupported(List<Contact> contacts, Vector3d up) {
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (contact.depth > -CONTACT_SKIN && contact.normal.dot(up) > SUPPORT_DOT) {
                return true;
            }
        }
        return false;
    }

    
    
    

    













    private void solveVelocities(List<Contact> contacts, double dt) {
        if (contacts.isEmpty()) {
            return;
        }
        prepareContacts(contacts, dt);
        for (int iteration = 0; iteration < VELOCITY_ITERATIONS; iteration++) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                solveNormal(contact);
                solveFriction(contact);
            }
        }
    }

    





    private void prepareContacts(List<Contact> contacts, double dt) {
        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).prepare(this, dt);
        }
    }

    




















    private void applyRestitution(List<Contact> contacts) {
        double threshold = impactThreshold(RESTITUTION_THRESHOLD_TICKS);
        for (int iteration = 0; iteration < RESTITUTION_ITERATIONS; iteration++) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                if (contact.restitution <= 0.0D
                        || contact.normalImpulse <= 0.0D
                        || contact.approachSpeed > -threshold) {
                    continue;
                }
                Vector3d relative = pointVelocity(contact.r).subtract(contact.otherPointVelocity());
                double lambda = (-contact.restitution * contact.approachSpeed - relative.dot(contact.normal))
                        / contact.normalMass;
                
                
                double accumulated = Math.max(0.0D, contact.normalImpulse + lambda);
                lambda = accumulated - contact.normalImpulse;
                contact.normalImpulse = accumulated;
                applyContactImpulse(contact, contact.normal.scale(lambda));
            }
        }
    }

    































    private void applyImpactScatter(List<Contact> contacts) {
        double threshold = impactThreshold(SCATTER_THRESHOLD_TICKS);
        Contact hit = null;
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (contact.normalImpulse <= 0.0D || contact.approachSpeed > -threshold) {
                continue;
            }
            if (hit == null || contact.approachSpeed < hit.approachSpeed) {
                hit = contact;
            }
        }
        if (hit == null) {
            return;
        }
        
        
        double pressed = 0.0D;
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (contact.normal.dot(hit.normal) > 0.99D) {
                pressed += contact.normalImpulse;
            }
        }

        Vector3d normal = hit.normal;
        Vector3d slide = this.linearVelocity.subtract(normal.scale(this.linearVelocity.dot(normal)));
        double sliding = slide.length();
        double speed = Math.min(SCATTER_GAIN * -hit.approachSpeed, SCATTER_MAX);
        speed = Math.min(speed, hit.friction * pressed / MASS);
        speed = Math.min(speed, SCATTER_MAX - sliding);
        if (speed <= 0.0D) {
            return;
        }

        Vector3d along = sliding > SCATTER_SLIDE_EPSILON ? slide.scale(1.0D / sliding) : randomTangent(normal);
        
        
        double span = (this.halfX + this.halfY + this.halfZ) / 3.0D;
        this.linearVelocity = this.linearVelocity.add(along.scale(speed));
        this.angularVelocity = clampSpeed(
                this.angularVelocity.add(normal.cross(along).scale(SCATTER_TUMBLE * speed / span)),
                MAX_ANGULAR_SPEED);
    }

    
    private Vector3d randomTangent(Vector3d normal) {
        Vector3d reference = Math.abs(normal.y) < 0.9D
                ? new Vector3d(0.0D, 1.0D, 0.0D) : new Vector3d(1.0D, 0.0D, 0.0D);
        Vector3d first = normal.cross(reference).normalize();
        Vector3d second = normal.cross(first);
        double angle = this.random.nextDouble() * (Math.PI * 2.0D);
        return first.scale(Math.cos(angle)).add(second.scale(Math.sin(angle)));
    }

    private void solveNormal(Contact contact) {
        Vector3d relative = pointVelocity(contact.r).subtract(contact.otherPointVelocity());
        double approach = relative.dot(contact.normal);
        double lambda = (contact.targetNormalSpeed - approach) / contact.normalMass;
        double accumulated = Math.max(0.0D, contact.normalImpulse + lambda);
        lambda = accumulated - contact.normalImpulse;
        contact.normalImpulse = accumulated;
        applyContactImpulse(contact, contact.normal.scale(lambda));
    }

    private void solveFriction(Contact contact) {
        if (contact.normalImpulse <= 0.0D || contact.depth < -CONTACT_SKIN) {
            
            
            return;
        }
        Vector3d relative = pointVelocity(contact.r).subtract(contact.otherPointVelocity());
        double limit = contact.friction * contact.normalImpulse;

        double lambda1 = -relative.dot(contact.tangent1) / contact.tangentMass1;
        double lambda2 = -relative.dot(contact.tangent2) / contact.tangentMass2;
        double newImpulse1 = contact.tangentImpulse1 + lambda1;
        double newImpulse2 = contact.tangentImpulse2 + lambda2;
        double magnitude = Math.sqrt(newImpulse1 * newImpulse1 + newImpulse2 * newImpulse2);
        if (magnitude > limit && magnitude > 0.0D) {
            
            
            double scale = limit / magnitude;
            newImpulse1 *= scale;
            newImpulse2 *= scale;
        }
        Vector3d impulse = contact.tangent1.scale(newImpulse1 - contact.tangentImpulse1)
                .add(contact.tangent2.scale(newImpulse2 - contact.tangentImpulse2));
        contact.tangentImpulse1 = newImpulse1;
        contact.tangentImpulse2 = newImpulse2;
        applyContactImpulse(contact, impulse);
    }

    private void applyContactImpulse(Contact contact, Vector3d impulse) {
        this.linearVelocity = this.linearVelocity.add(impulse.scale(1.0D / MASS));
        this.angularVelocity = this.angularVelocity.add(applyInverseInertia(contact.r.cross(impulse)));
        contact.applyToOther(impulse);
    }

    










    private Vector3d solvePositions(List<Contact> contacts, double dt) {
        if (contacts.isEmpty()) {
            return Vector3d.ZERO;
        }
        boolean any = false;
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            contact.positionImpulse = 0.0D;
            any |= contact.depth > PENETRATION_SLOP;
        }
        if (!any) {
            return Vector3d.ZERO;
        }

        Vector3d pseudoLinear = Vector3d.ZERO;
        Vector3d pseudoAngular = Vector3d.ZERO;
        for (int iteration = 0; iteration < POSITION_ITERATIONS; iteration++) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                if (contact.depth <= PENETRATION_SLOP) {
                    continue;
                }
                double target = Math.min(contact.depth - PENETRATION_SLOP, MAX_CORRECTION) * POSITION_BIAS / dt;
                Vector3d relative = pseudoLinear.add(pseudoAngular.cross(contact.r));
                double lambda = (target - relative.dot(contact.normal)) / contact.normalMass;
                double accumulated = Math.max(0.0D, contact.positionImpulse + lambda);
                lambda = accumulated - contact.positionImpulse;
                contact.positionImpulse = accumulated;
                pseudoLinear = pseudoLinear.add(contact.normal.scale(lambda / MASS));
                pseudoAngular = pseudoAngular.add(applyInverseInertia(contact.r.cross(contact.normal.scale(lambda))));
            }
        }
        integrateRotation(pseudoAngular, dt);
        return pseudoLinear.scale(dt);
    }

    
    
    

    





























    private List<Contact> collectContacts(Entity cube, Vector3d com, List<CubePhysics.Obstacle> obstacles, double dt) {
        List<Contact> contacts = new ArrayList<>();
        if (obstacles.isEmpty()) {
            return contacts;
        }
        
        
        
        
        double radius = Math.sqrt(this.halfX * this.halfX + this.halfY * this.halfY + this.halfZ * this.halfZ);
        double reach = CONTACT_SKIN
                + (this.linearVelocity.length() + this.angularVelocity.length() * radius) * dt;
        AxisAlignedBB span = new AxisAlignedBB(
                com.x - radius, com.y - radius, com.z - radius,
                com.x + radius, com.y + radius, com.z + radius).inflate(reach);

        for (int index = 0; index < obstacles.size(); index++) {
            CubePhysics.Obstacle obstacle = obstacles.get(index);
            if (!span.intersects(obstacle.box)) {
                continue;
            }
            collectManifold(contacts, com, obstacle, reach, dt);
            if (contacts.size() >= MAX_CONTACTS) {
                break;
            }
        }
        return contacts;
    }

    















    private double pointReach(Vector3d point, Vector3d com, Vector3d normal, CubePhysics.Obstacle obstacle, double dt) {
        Vector3d relative = pointVelocity(point.subtract(com)).subtract(obstacle.pointVelocity(point));
        
        
        double closing = -relative.dot(normal);
        return closing > 0.0D ? CONTACT_SKIN + closing * dt : CONTACT_SKIN;
    }

    



    private void collectManifold(List<Contact> contacts, Vector3d com, CubePhysics.Obstacle obstacle, double reach,
                                 double dt) {
        Vector3d otherCentre = obstacle.centre;
        Vector3d[] otherAxes = obstacle.axes;
        double[] otherHalf = obstacle.half;
        Vector3d[] cubeAxes = {this.axisX, this.axisY, this.axisZ};
        double[] cubeHalf = {this.halfX, this.halfY, this.halfZ};
        Vector3d delta = com.subtract(otherCentre);

        double bestScore = Double.POSITIVE_INFINITY;
        double bestOverlap = 0.0D;
        int bestIndex = -1;
        Vector3d bestNormal = null;
        for (int index = 0; index < SEPARATION_AXIS_COUNT; index++) {
            Vector3d axis = separationAxis(index, otherAxes);
            if (axis == null) {
                continue;
            }
            double overlap = projectionRadius(axis, cubeAxes, cubeHalf)
                    + projectionRadius(axis, otherAxes, otherHalf) - Math.abs(delta.dot(axis));
            if (overlap < -reach) {
                
                return;
            }
            
            
            Vector3d normal = delta.dot(axis) < 0.0D ? axis.scale(-1.0D) : axis;
            if (obstacle.isFaceBlocked(blockedFaceFor(normal, otherAxes))) {
                
                
                
                continue;
            }
            
            
            
            double score = index >= 6 ? overlap + EDGE_AXIS_BIAS : overlap;
            if (score < bestScore) {
                bestScore = score;
                bestOverlap = overlap;
                bestIndex = index;
                bestNormal = normal;
            }
        }
        if (bestIndex < 0) {
            return;
        }

        if (bestIndex >= 6) {
            addEdgeContact(contacts, com, obstacle, bestIndex, bestNormal, bestOverlap, otherCentre, otherAxes, otherHalf);
            return;
        }
        if (bestIndex < 3) {
            
            Vector3d refCentre = otherCentre.add(bestNormal.scale(otherHalf[bestIndex]));
            int first = (bestIndex + 1) % 3;
            int second = (bestIndex + 2) % 3;
            addClippedContacts(contacts, com, obstacle, bestNormal,
                    refCentre, bestNormal, otherAxes[first], otherHalf[first], otherAxes[second], otherHalf[second],
                    incidentFace(bestNormal, com, cubeAxes, cubeHalf), dt);
        }
        else {
            
            Vector3d refNormal = bestNormal.scale(-1.0D);
            int cubeAxis = bestIndex - 3;
            Vector3d refCentre = com.add(refNormal.scale(cubeHalf[cubeAxis]));
            int first = (cubeAxis + 1) % 3;
            int second = (cubeAxis + 2) % 3;
            addClippedContacts(contacts, com, obstacle, bestNormal,
                    refCentre, refNormal, cubeAxes[first], cubeHalf[first], cubeAxes[second], cubeHalf[second],
                    incidentFace(refNormal, otherCentre, otherAxes, otherHalf), dt);
        }
    }

    



    private static Vector3d[] incidentFace(Vector3d towards, Vector3d centre, Vector3d[] axes, double[] half) {
        int best = 0;
        double bestDot = -1.0D;
        for (int i = 0; i < 3; i++) {
            double dot = Math.abs(axes[i].dot(towards));
            if (dot > bestDot) {
                bestDot = dot;
                best = i;
            }
        }
        
        double sign = axes[best].dot(towards) > 0.0D ? -1.0D : 1.0D;
        Vector3d faceCentre = centre.add(axes[best].scale(sign * half[best]));
        int first = (best + 1) % 3;
        int second = (best + 2) % 3;
        Vector3d u = axes[first].scale(half[first]);
        Vector3d v = axes[second].scale(half[second]);
        return new Vector3d[]{
                faceCentre.add(u).add(v),
                faceCentre.subtract(u).add(v),
                faceCentre.subtract(u).subtract(v),
                faceCentre.add(u).subtract(v)};
    }

    








    private void addClippedContacts(List<Contact> contacts, Vector3d com, CubePhysics.Obstacle obstacle,
                                    Vector3d normal, Vector3d refCentre, Vector3d refNormal,
                                    Vector3d refU, double halfU, Vector3d refV, double halfV,
                                    Vector3d[] incident, double dt) {
        List<Vector3d> polygon = new ArrayList<>(8);
        for (Vector3d point : incident) {
            polygon.add(point);
        }
        polygon = clipToSlab(polygon, refCentre, refU, halfU);
        polygon = clipToSlab(polygon, refCentre, refV, halfV);
        for (int i = 0; i < polygon.size(); i++) {
            Vector3d point = polygon.get(i);
            double depth = -point.subtract(refCentre).dot(refNormal);
            if (depth > -pointReach(point, com, normal, obstacle, dt)) {
                addContact(contacts, new Contact(point, normal, depth, com, obstacle));
                if (contacts.size() >= MAX_CONTACTS) {
                    return;
                }
            }
        }
    }

    
    private static List<Vector3d> clipToSlab(List<Vector3d> polygon, Vector3d centre, Vector3d axis, double half) {
        return clipHalfSpace(clipHalfSpace(polygon, centre, axis, half), centre, axis.scale(-1.0D), half);
    }

    private static List<Vector3d> clipHalfSpace(List<Vector3d> polygon, Vector3d centre, Vector3d axis, double half) {
        if (polygon.isEmpty()) {
            return polygon;
        }
        List<Vector3d> out = new ArrayList<>(polygon.size() + 2);
        for (int i = 0; i < polygon.size(); i++) {
            Vector3d from = polygon.get(i);
            Vector3d to = polygon.get((i + 1) % polygon.size());
            double fromSide = from.subtract(centre).dot(axis) - half;
            double toSide = to.subtract(centre).dot(axis) - half;
            if (fromSide <= 0.0D) {
                out.add(from);
            }
            if ((fromSide > 0.0D) != (toSide > 0.0D)) {
                double along = fromSide / (fromSide - toSide);
                out.add(from.add(to.subtract(from).scale(along)));
            }
        }
        return out;
    }

    



    private void addEdgeContact(List<Contact> contacts, Vector3d com, CubePhysics.Obstacle obstacle,
                                int axisIndex, Vector3d normal, double overlap,
                                Vector3d otherCentre, Vector3d[] otherAxes, double[] otherHalf) {
        Vector3d[] cubeAxes = {this.axisX, this.axisY, this.axisZ};
        double[] cubeHalf = {this.halfX, this.halfY, this.halfZ};
        int cubeIndex = (axisIndex - 6) / 3;
        int worldIndex = (axisIndex - 6) % 3;

        
        
        Vector3d cubePoint = com;
        for (int i = 0; i < 3; i++) {
            if (i != cubeIndex) {
                cubePoint = cubePoint.add(cubeAxes[i].scale(-Math.signum(normal.dot(cubeAxes[i])) * cubeHalf[i]));
            }
        }
        Vector3d otherPoint = otherCentre;
        for (int i = 0; i < 3; i++) {
            if (i != worldIndex) {
                otherPoint = otherPoint.add(otherAxes[i].scale(Math.signum(normal.dot(otherAxes[i])) * otherHalf[i]));
            }
        }
        Vector3d point = closestPointBetweenEdges(
                cubePoint, cubeAxes[cubeIndex], cubeHalf[cubeIndex],
                otherPoint, otherAxes[worldIndex], otherHalf[worldIndex]);
        addContact(contacts, new Contact(point, normal, overlap, com, obstacle));
    }

    
    private static Vector3d closestPointBetweenEdges(Vector3d centreA, Vector3d dirA, double halfA,
                                                     Vector3d centreB, Vector3d dirB, double halfB) {
        Vector3d between = centreA.subtract(centreB);
        double alignment = dirA.dot(dirB);
        double alongA = dirA.dot(between);
        double alongB = dirB.dot(between);
        double denominator = 1.0D - alignment * alignment;
        double a;
        double b;
        if (denominator > 1.0E-9D) {
            a = (alignment * alongB - alongA) / denominator;
            b = (alongB - alignment * alongA) / denominator;
        }
        else {
            
            a = 0.0D;
            b = 0.0D;
        }
        a = MathHelper.clamp(a, -halfA, halfA);
        b = MathHelper.clamp(b, -halfB, halfB);
        return centreA.add(dirA.scale(a)).add(centreB.add(dirB.scale(b))).scale(0.5D);
    }

    







    private static void addContact(List<Contact> contacts, Contact contact) {
        for (int i = 0; i < contacts.size(); i++) {
            Contact existing = contacts.get(i);
            if (existing.normal.dot(contact.normal) > 0.999D
                    && existing.point.distanceToSqr(contact.point) < 1.0E-8D) {
                if (contact.depth > existing.depth) {
                    
                    
                    contacts.set(i, contact);
                }
                return;
            }
        }
        contacts.add(contact);
    }

    
    private static final int SEPARATION_AXIS_COUNT = 15;

    private Vector3d separationAxis(int index, Vector3d[] otherAxes) {
        if (index < 3) {
            return otherAxes[index];
        }
        if (index < 6) {
            return index == 3 ? this.axisX : (index == 4 ? this.axisY : this.axisZ);
        }
        int own = (index - 6) / 3;
        int other = (index - 6) % 3;
        Vector3d ownAxis = own == 0 ? this.axisX : (own == 1 ? this.axisY : this.axisZ);
        Vector3d cross = ownAxis.cross(otherAxes[other]);
        return cross.lengthSqr() < 1.0E-10D ? null : cross.normalize();
    }

    
    private static double projectionRadius(Vector3d axis, Vector3d[] axes, double[] half) {
        return Math.abs(axis.dot(axes[0])) * half[0]
                + Math.abs(axis.dot(axes[1])) * half[1]
                + Math.abs(axis.dot(axes[2])) * half[2];
    }

    



    private static int blockedFaceFor(Vector3d normal, Vector3d[] otherAxes) {
        for (int axis = 0; axis < 3; axis++) {
            double along = normal.dot(otherAxes[axis]);
            if (Math.abs(Math.abs(along) - 1.0D) < 1.0E-6D) {
                return axis * 2 + (along > 0.0D ? 1 : 0);
            }
        }
        return -1;
    }


    
    
    

    







    void applyExternalImpulse(Vector3d com, Vector3d point, Vector3d impulse) {
        Vector3d change = impulse.scale(1.0D / MASS);
        this.pendingImpulse = this.pendingImpulse.add(change);
        this.linearVelocity = this.linearVelocity.add(change);
        this.angularVelocity = clampSpeed(
                this.angularVelocity.add(applyInverseInertia(point.subtract(com).cross(impulse))), MAX_ANGULAR_SPEED);
        disturb();
    }

    
    Vector3d getLinearVelocity() {
        return this.linearVelocity;
    }

    private static Vector3d clampSpeed(Vector3d vector, double limit) {
        double lengthSqr = vector.lengthSqr();
        if (lengthSqr <= limit * limit || lengthSqr == 0.0D) {
            return vector;
        }
        return vector.scale(limit / Math.sqrt(lengthSqr));
    }

    private static Vector3d sanitise(Vector3d vector) {
        if (Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z)) {
            return vector;
        }
        return Vector3d.ZERO;
    }

    
    
    

    
    Vector3d takeSolvedVelocity() {
        Vector3d velocity = this.solvedVelocity;
        this.solvedVelocity = null;
        return velocity;
    }

    boolean isGrounded() {
        return this.grounded;
    }

    






    boolean needsSync() {
        if (!this.everSent || this.sentControlling != this.controlling) {
            return true;
        }
        return Math.abs(this.sentX - this.qx) > SYNC_EPSILON
                || Math.abs(this.sentY - this.qy) > SYNC_EPSILON
                || Math.abs(this.sentZ - this.qz) > SYNC_EPSILON
                || Math.abs(this.sentW - this.qw) > SYNC_EPSILON;
    }

    void markSynced() {
        this.everSent = true;
        this.sentControlling = this.controlling;
        this.sentX = this.qx;
        this.sentY = this.qy;
        this.sentZ = this.qz;
        this.sentW = this.qw;
    }

    float orientationX() {
        return (float) this.qx;
    }

    float orientationY() {
        return (float) this.qy;
    }

    float orientationZ() {
        return (float) this.qz;
    }

    float orientationW() {
        return (float) this.qw;
    }

    






    void acceptFromNetwork(double x, double y, double z, double w, Vector3d spin, boolean controlling) {
        Sample sample = new Sample(x, y, z, w, spin, controlling);
        if (!this.primed) {
            this.primed = true;
            adopt(sample);
            snapshotOrientation();
            return;
        }
        if (this.playout == null) {
            this.playout = new ArrayDeque<>(PLAYOUT_CAPACITY);
        }
        if (this.playout.size() >= PLAYOUT_CAPACITY) {
            
            
            
            this.playout.removeFirst();
            this.snapNext = true;
        }
        this.playout.addLast(sample);
    }

    

















    void advanceClientTick() {
        snapshotOrientation();
        if (this.playout == null || this.playout.isEmpty()) {
            
            
            return;
        }
        Sample sample = this.playout.pollFirst();
        if (this.playout.size() >= PLAYOUT_CATCHUP) {
            
            
            
            adopt(sample);
            snapshotOrientation();
            sample = this.playout.pollFirst();
        }
        adopt(sample);
        if (this.snapNext) {
            this.snapNext = false;
            snapshotOrientation();
        }
    }

    private void adopt(Sample sample) {
        setOrientation(sample.x, sample.y, sample.z, sample.w);
        this.angularVelocity = sample.spin;
        this.controlling = sample.controlling;
    }

    
    
    

    void save(CompoundNBT tag) {
        tag.putDouble("QX", this.qx);
        tag.putDouble("QY", this.qy);
        tag.putDouble("QZ", this.qz);
        tag.putDouble("QW", this.qw);
        tag.putDouble("SpinX", this.angularVelocity.x);
        tag.putDouble("SpinY", this.angularVelocity.y);
        tag.putDouble("SpinZ", this.angularVelocity.z);
        tag.putBoolean("Controlling", this.controlling);
    }

    void load(CompoundNBT tag) {
        setOrientation(tag.getDouble("QX"), tag.getDouble("QY"), tag.getDouble("QZ"), tag.getDouble("QW"));
        snapshotOrientation();
        this.angularVelocity = clampSpeed(
                new Vector3d(tag.getDouble("SpinX"), tag.getDouble("SpinY"), tag.getDouble("SpinZ")),
                MAX_ANGULAR_SPEED);
        this.controlling = tag.getBoolean("Controlling");
        this.resting = false;
        this.restTicks = 0;
    }

    
    
    

    






    private static final class Sample {

        final double x, y, z, w;
        final Vector3d spin;
        final boolean controlling;

        Sample(double x, double y, double z, double w, Vector3d spin, boolean controlling) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.spin = spin;
            this.controlling = controlling;
        }
    }

    
    
    

    





    private static final class Contact {

        final Vector3d normal;
        final Vector3d r;
        final double depth;
        final double friction;
        final double restitution;
        final CubePhysics.Obstacle obstacle;
        final Vector3d point;

        Vector3d tangent1 = Vector3d.ZERO;
        Vector3d tangent2 = Vector3d.ZERO;
        double normalMass = 1.0D;
        double tangentMass1 = 1.0D;
        double tangentMass2 = 1.0D;
        double targetNormalSpeed;

        
        double approachSpeed;

        double normalImpulse;
        double tangentImpulse1;
        double tangentImpulse2;
        double positionImpulse;
        boolean otherIsBraced;

        Contact(Vector3d point, Vector3d normal, double depth, Vector3d com, CubePhysics.Obstacle obstacle) {
            this.point = point;
            this.normal = normal;
            
            
            this.depth = depth + REST_GAP;
            this.r = point.subtract(com);
            this.friction = obstacle.friction;
            this.restitution = obstacle.restitution;
            this.obstacle = obstacle;
        }

        




        void prepare(CubeRigidBody body, double dt) {
            
            
            
            
            
            
            
            
            this.otherIsBraced = this.obstacle.isBracedAgainst(this.normal);
            this.normalMass = body.effectiveMass(this.r, this.normal) + otherEffectiveMass(this.normal);
            Vector3d relative = body.pointVelocity(this.r).subtract(otherPointVelocity());
            Vector3d slide = relative.subtract(this.normal.scale(relative.dot(this.normal)));
            if (slide.lengthSqr() > 1.0E-12D) {
                this.tangent1 = slide.normalize();
            }
            else {
                Vector3d reference = Math.abs(this.normal.y) < 0.9D
                        ? new Vector3d(0.0D, 1.0D, 0.0D) : new Vector3d(1.0D, 0.0D, 0.0D);
                this.tangent1 = this.normal.cross(reference).normalize();
            }
            this.tangent2 = this.normal.cross(this.tangent1).normalize();
            this.tangentMass1 = body.effectiveMass(this.r, this.tangent1) + otherEffectiveMass(this.tangent1);
            this.tangentMass2 = body.effectiveMass(this.r, this.tangent2) + otherEffectiveMass(this.tangent2);

            
            
            
            
            
            
            
            
            
            this.approachSpeed = relative.dot(this.normal);
            this.targetNormalSpeed = this.depth < 0.0D ? this.depth / dt : 0.0D;
        }

        private double otherEffectiveMass(Vector3d direction) {
            return this.otherIsBraced ? 0.0D : this.obstacle.effectiveMass(this.point, direction);
        }

        



        Vector3d otherPointVelocity() {
            return this.otherIsBraced ? Vector3d.ZERO : this.obstacle.pointVelocity(this.point);
        }

        














        void applyToOther(Vector3d impulse) {
            if (!this.otherIsBraced || !this.obstacle.isAsleep()) {
                this.obstacle.receiveImpulse(this.point, impulse.scale(-1.0D));
            }
        }
    }
}
