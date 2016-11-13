package org.usfirst.frc.team3853.robot;

import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This is a demo program showing the use of the RobotDrive class, specifically
 * it contains the code necessary to operate a robot with tank drive.
 *
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SampleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 *
 * WARNING: While it may look like a good choice to use for your code if you're
 * inexperienced, don't. Unless you know what you are doing, complex code will
 * be much more difficult under this system. Use IterativeRobot or Command-Based
 * instead if you're new.
 */
public class Robot extends SampleRobot {

  private RobotDrive myRobotDrive; // object that handles basic drive operations

  private Joystick jStick; // set to ID 1 in DriverStation

  private PowerDistributionPanel pdp;  // what's happening to the battery & fuses?

  // the RoboRIO has a three axis accelerometer builtin; use it
  private BuiltInAccelerometer accel;

  // this is an instance a configurable helper class that averages some 
  // number of observed measurements of tyoe double
  // here we will measure what the current value of "down" is
  private Average avgG;

  // timer object to manage loop timing
  private Timer loopTimer;
  private final double teleopLoopTime = 0.02;  // teleop about as fast as network updates
  private final double autonLoopTime = 0.005;  // autonomous quick to react
  private final double shortDelay = Math.min( teleopLoopTime, autonLoopTime ) / 10.0;

  private double lastTimeMark;  // the last time something interesting happened

  // there will be a few things going on in sequence in autonomous
  // this defines the legal states of the machine
  enum AutonState {
    FirstWait, Approach, Breach, Courtyard, Stop
  }
  // and this is where the current state of the robot is remembered
  // initialized appropriately here, but also at the begining of autonomous
  // this helps when testing, so the robot code does not need to be restarted to rerun auton
  private AutonState autoState = AutonState.FirstWait;

  // just watching the Earth's gravitational pull on the robot.
  private boolean botIsLevel = true;

  // counter used to slow down updates back to the meatware
  private int i;

  // final declarations are aids to the compiler to assist in optimizations
  // if the value is not going to change on the fly, this is more efficient
  
  // how picky are we about the measurement of gravity, Z axis on the accelerometer
  private final double closeEnoughToOneG = 0.005;  // 0.5%

  // drivetrain speed constants, usual +1 -> -1 range
  private final double speedApproach = 0.3;  // how hard to go on the flats
  private final double speedBreach   = 1.0;  // how hard to get over the defense

  // control state timeout values, in seconds; sum not to exceed 15
  private final double timeWait      = 1.0;  // don't be anxious, start a little late
  private final double timeApproach  = 2.0;  // not to exceed time to find an obstacle
  private final double timeBreach    = 5.0;  // not to exceed time to scale a defense
  private final double timeCourtyard = 2.0;  // move into the castle a little once over the walls

  
  public Robot() {
    
    myRobotDrive = new RobotDrive( 0, 1 );
    myRobotDrive.setExpiration( 0.1 );

    jStick = new Joystick( 0 );

    pdp = new PowerDistributionPanel();
    LiveWindow.addSensor( null, "PDP", pdp );

    // this is a helper object. It keeps track of the last few measurements (first parameter)
    // the initial state is set from the seond parameter
    avgG = new Average( 8, 1.0 );
    
    loopTimer = new Timer();

    SmartDashboard.putString( "RobotID", "ArcadeDriveSaple 161113a" );

    SmartDashboard.putNumber( "Joystick POV", jStick.getPOV() );
    SmartDashboard.putString( "Joystick name", jStick.getName() );
    SmartDashboard.putNumber( "Joystick type", jStick.getType() );
  }

  public void operatorControl() {
    i = 0;
    loopTimer.reset();
    myRobotDrive.setSafetyEnabled( true );

    // operator control loop
    //   control motors from joystick
    //   watch the accelerometer
    //   update the dashboard with interesting information accasionally
    while (isOperatorControl() && isEnabled()) {

      myRobotDrive.arcadeDrive( jStick );

      avgG.sampleAdd( accel.getZ() );

      if ( Math.floorMod( i, 200 ) == 0 ) {
        SmartDashboard.putNumber( "Amps", pdp.getTotalCurrent() );
        SmartDashboard.putNumber( "Volts", pdp.getVoltage() );
        SmartDashboard.putNumber( "G", avgG.sampleAverage() );

        SmartDashboard.putNumber( "Joystick POV", jStick.getPOV() );
      }

      while ( !loopTimer.hasPeriodPassed( autonLoopTime ))
        Timer.delay( shortDelay );

      i+=1;
    }  // end operator control loop

    myRobotDrive.stopMotor();
  }  //  end operatorControl

  public void testPeriodic() {
    LiveWindow.run();
  }

  public void autonomous() {
    double timeMark;
    double averagedG = 1.0;  // assume start on a flat surface
    i = 0;  //reused i as a counter; dubious code or time savings]
    
    loopTimer.reset();
    lastTimeMark = Timer.getFPGATimestamp();  // when did this start?
    myRobotDrive.arcadeDrive( 0.0, 0.0 );  // don't move yet
    autoState = AutonState.FirstWait;

    while (isAutonomous() && isEnabled() && (autoState != AutonState.Stop)) {
      // get the current time
      timeMark = Timer.getFPGATimestamp();

      // where's down?
      // run the gravity averaging routine
      avgG.sampleAdd( accel.getY() );
      averagedG = avgG.sampleAverage();

      // is the Bot on the field, or dealing with a defense?
      if ( Math.abs( averagedG - 1.0 ) < closeEnoughToOneG )
        // on the field
        botIsLevel = true;
      else
        // somehow tilted
        botIsLevel = false;

      // push data back to the humans every now and then
      if ( Math.floorMod( i, 20 ) == 0 ) {
        SmartDashboard.putBoolean( "LEVEL", botIsLevel );
        SmartDashboard.putNumber( "G", averagedG );
      }

      // STATE MACHINE: decide what to do next
      switch ( autoState ) {
        case FirstWait: {
          // wait a moment before moving
          if ( timeMark > (lastTimeMark + timeWait) ) {
            // start moving
            lastTimeMark = timeMark;
            myRobotDrive.arcadeDrive( speedApproach, 0.0 );
            autoState = AutonState.Approach;
          }
          break;
        }
        case Approach: {
          // move until the robot is not on a flat floor
          if ( !botIsLevel ) {
            lastTimeMark = timeMark;
            myRobotDrive.arcadeDrive( speedBreach, 0.0 );
            autoState = AutonState.Breach;
          } else if ( timeMark > (lastTimeMark + timeApproach) ) {
            // there's only so much patience; quit if it takes too long to find
            // a defense. (how the heck did it start running down the secret passage?)
            myRobotDrive.arcadeDrive( 0.0, 0.0 );
            autoState = AutonState.Stop;
          }
          break;
        }
        case Breach: {
          // move at another speed until the robot is levekl again
          if ( botIsLevel ) {
            lastTimeMark = timeMark;
            myRobotDrive.arcadeDrive( speedApproach, 0.0 );
            autoState = AutonState.Courtyard;
          } else if ( timeMark > (lastTimeMark + timeBreach) ) {
            // but it seems to be not making progress, quit; let the humans fix
            // it in teleop
            myRobotDrive.arcadeDrive( 0.0, 0.0 );
            autoState = AutonState.Stop;
          }
          break;
        }
        case Courtyard: {
          // past the defense, drive a little farther
          if ( timeMark > (lastTimeMark + timeCourtyard) ) {
            // but only for a little while
            lastTimeMark = timeMark;
            myRobotDrive.arcadeDrive( 0.0, 0.0 );
            autoState = AutonState.Stop;
          }
          break;
        }
        case Stop: {
          myRobotDrive.stopMotor();
          break;
        }
        default: {
          // something is very wrong. STOP!!! and stay stopped
          autoState = AutonState.Stop;
          myRobotDrive.stopMotor();
        }
      }  // end state machine switch statement

      while ( !loopTimer.hasPeriodPassed( autonLoopTime ))
        Timer.delay( shortDelay );
      i += 1; 
    }  //  end autonomous timing loop

    // probably superfluous, but shut down the drive train
    myRobotDrive.stopMotor();
  }  //  end autonomous

}  //  end class Robot
