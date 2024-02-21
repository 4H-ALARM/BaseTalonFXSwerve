package frc.robot.swervemodules;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.lib.util.Conversions;
import frc.lib.util.SwerveModuleConstants;
import frc.lib.doubleNeo.doubleNeoConstants;
import frc.robot.Robot;
import frc.robot.interfaces.SwerveModule;

public class SwerveModuleKrakenFalcon implements SwerveModule {
    private final Rotation2d angleOffset;

    private final TalonFX mAngleMotor;
    private final TalonFX mDriveMotor;
    private final CANcoder angleEncoder;
    private final int mModule;

    private final SimpleMotorFeedforward driveFeedForward = new SimpleMotorFeedforward(doubleNeoConstants.Swerve.driveKS, doubleNeoConstants.Swerve.driveKV, doubleNeoConstants.Swerve.driveKA);

    /* drive motor control requests */
    private final DutyCycleOut driveDutyCycle = new DutyCycleOut(0);
    private final VelocityVoltage driveVelocity = new VelocityVoltage(0);

    /* angle motor control requests */
    private final PositionVoltage anglePosition = new PositionVoltage(0);

    public SwerveModuleKrakenFalcon(SwerveModuleConstants moduleConstants, int moduleNumber) {
        this.angleOffset = moduleConstants.angleOffset;
        this.mModule = moduleNumber;

        /* Angle Encoder Config */
        angleEncoder = new CANcoder(moduleConstants.cancoderID);
        angleEncoder.getConfigurator().apply(Robot.ctreConfigs.swerveCANcoderConfig);

        /* Angle Motor Config */
        mAngleMotor = new TalonFX(moduleConstants.angleMotorID);
        mAngleMotor.getConfigurator().apply(Robot.ctreConfigs.swerveAngleFXConfig);
        resetToAbsolute();

        /* Drive Motor Config */
        mDriveMotor = new TalonFX(moduleConstants.driveMotorID);
        mDriveMotor.getConfigurator().apply(Robot.ctreConfigs.swerveDriveFXConfig);
        mDriveMotor.getConfigurator().setPosition(0.0);
    }

    @Override
    public void setDesiredState(SwerveModuleState desiredState, boolean isOpenLoop) {
        desiredState = SwerveModuleState.optimize(desiredState, getState().angle);
        mAngleMotor.setControl(anglePosition.withPosition(desiredState.angle.getRotations()));
        setSpeed(desiredState, isOpenLoop);
    }

    @Override
    public void debugSetDriveSpeed(double speed) {
        mDriveMotor.set(speed);
    }

    @Override
    public void debugSetSteeringSpeed(double speed) {
        mAngleMotor.set(speed);
    }

    @Override
    public Rotation2d getRotation() {
        return Rotation2d.fromRotations(
//                angleEncoder.getPosition().getValue()
                angleEncoder.getAbsolutePosition().getValue()
        );
    }

    @Override
    public void resetToAbsolute() {
        double absolutePosition = getRotation().getRotations() - angleOffset.getRotations();
        mAngleMotor.setPosition(absolutePosition);
        System.out.printf("Absolute Position: %f\n", absolutePosition);
        System.out.printf("Cancoder: %f\n", getRotation().getRotations());
    }

    @Override
    public SwerveModuleState getState() {
        return new SwerveModuleState(
                Conversions.RPSToMPS(mDriveMotor.getVelocity().getValue(), doubleNeoConstants.Swerve.wheelCircumference),
                Rotation2d.fromRotations(mAngleMotor.getPosition().getValue())
        );
    }

    @Override
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(
                Conversions.rotationsToMeters(mDriveMotor.getPosition().getValue(), doubleNeoConstants.Swerve.wheelCircumference),
                Rotation2d.fromRotations(mAngleMotor.getPosition().getValue())
        );
    }


    private void setSpeed(SwerveModuleState desiredState, boolean isOpenLoop) {
        if(isOpenLoop){
            driveDutyCycle.Output = desiredState.speedMetersPerSecond / doubleNeoConstants.Swerve.maxSpeed;
            mDriveMotor.setControl(driveDutyCycle);
        }
        else {
            driveVelocity.Velocity = Conversions.MPSToRPS(desiredState.speedMetersPerSecond, doubleNeoConstants.Swerve.wheelCircumference);
            driveVelocity.FeedForward = driveFeedForward.calculate(desiredState.speedMetersPerSecond);
            mDriveMotor.setControl(driveVelocity);
        }
    }

    private Rotation2d cancoderOffset = new Rotation2d();
    private Rotation2d motorOffset = new Rotation2d();
    public void zeroEncoders() {
        cancoderOffset = getRotation();
        motorOffset = Rotation2d.fromRotations(mAngleMotor.getPosition().getValue());
    }

    @Override
    public void dashboardPeriodic() {
        SmartDashboard.putNumber(String.format("CanCoder%d Angle", mModule), getRotation().getRotations());
        SmartDashboard.putNumber(String.format("MotorSteer%d Angle", mModule), Rotation2d.fromRotations(mAngleMotor.getPosition().getValue()).getRotations());

        Rotation2d adjustedCancoder = Rotation2d.fromRotations(getRotation().getRotations() - cancoderOffset.getRotations());
        Rotation2d adjustedMotor = Rotation2d.fromRotations(mAngleMotor.getPosition().getValue() - motorOffset.getRotations());

        SmartDashboard.putNumber(String.format("ZeroCanCoder%d Angle", mModule), adjustedCancoder.getRotations());
        SmartDashboard.putNumber(String.format("ZeroMotor%d Angle", mModule), adjustedMotor.getRotations());

//        SmartDashboard.putNumber(String.format("DriveMotor%d Voltage", mModule), mDriveMotor.getMotorVoltage().getValue());
//        SmartDashboard.putNumber(String.format("AngleMotor%d Voltage", mModule), mAngleMotor.getMotorVoltage().getValue());
    }
}
