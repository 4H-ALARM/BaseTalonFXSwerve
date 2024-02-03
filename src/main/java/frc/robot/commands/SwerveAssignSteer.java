package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.SwerveTest;

/**
 * Command that assigns a specific speed to the drive motors of the swerve drive
 */
public class SwerveAssignSteer extends Command {
    private static final double DEFAULT_SPEED = 0.50;
    private final double mSpeed;
    private final SwerveTest mSwerve;

    public SwerveAssignSteer(SwerveTest swerve) {
        this(swerve, DEFAULT_SPEED);
    }

    public SwerveAssignSteer(SwerveTest swerve, double speed) {
        this.mSwerve = swerve;
        this.mSpeed = speed;
    }

    public void initialize() {
        for (int i = 0; i < 4; i++) {
            mSwerve.setSteeringSpeed(i, mSpeed);
        }
    }

    public void execute() {
    }

    public boolean isFinished() {
        return false;
    }

    public void end(boolean interrupted) {
        for (int i = 0; i < 4; i++) {
            mSwerve.setSteeringSpeed(i, mSpeed);
        }
    }
}