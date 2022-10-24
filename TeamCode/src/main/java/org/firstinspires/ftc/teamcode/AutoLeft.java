/*
 * Copyright (c) 2020 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

 /*
  * PID controller and IMU codes are copied from
  * https://stemrobotics.cs.pdx.edu/node/7268%3Froot=4196.html
  */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.hardware.bosch.BNO055IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;


/**
 * This file contains an minimal example of a Linear "OpMode". An OpMode is a 'program' that runs in either
 * the autonomous or the teleop period of an FTC match. The names of OpModes appear on the menu
 * of the FTC Driver Station. When an selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 *
 * This particular OpMode just executes a basic Tank Drive Teleop for a two wheeled robot
 * It includes all the skeletal structure that all linear OpModes contain.
 *
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@Autonomous(name="AutoLeft", group="Concept")
//@Disabled
public class AutoLeft extends LinearOpMode {

    // Declare OpMode members.
    static final double MAX_WAIT_TIME = 30; // in seconds
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor FrontLeftDrive = null;
    private DcMotor FrontRightDrive = null;
    private DcMotor BackLeftDrive = null;
    private DcMotor BackRightDrive = null;
    private BNO055IMU imu = null;


    // Driving motor variables
    static final int RAMP_ON = 1; // ramp on to improve small moving control. On: 1; off: 0
    static final double POWER_FACTOR = 0.6;  // used to adjust driving sensitivity.
    static final double AUTO_DRIVE_POWER = 0.3; // used for auto driving

    // slider motor variables
    private DcMotor RightSliderMotor = null;
    private DcMotor LeftSliderMotor = null;
    static final double SLIDER_MOTOR_POWER = 0.6; // slider string gets loose with too high speed
    static final int COUNTS_PER_INCH = 115;
    static final int FOUR_STAGE_SLIDER_MAX_POS = 4200;  // Leave 100 counts for buffer.
    static final int SLIDER_MIN_POS = 0;
    static final int GROUND_JUNCTION_POS = 250;
    static final int READY_FOR_GRIP_POSITION = 700; // lift a little bit to get ready for unloading

    // 10inch for low junction, 20inch for medium, and 30 for high
    static final int LOW_JUNCTION_POS = COUNTS_PER_INCH * 10 + READY_FOR_GRIP_POSITION; // need double check by testing
    static final int MEDIUM_JUNCTION_POS = COUNTS_PER_INCH * 20 + READY_FOR_GRIP_POSITION;
    static final int HIGH_JUNCTION_POS = COUNTS_PER_INCH * 30 + READY_FOR_GRIP_POSITION;
    static final int SLIDER_MOVE_DOWN_POSITION = 700; // move down a little bit to unload cone
    static final int POSITION_COUNTS_FOR_ONE_REVOLUTION = 512; // Approximate value from testing
    int motorPositionInc = POSITION_COUNTS_FOR_ONE_REVOLUTION/40; // set value based on testing
    int sliderMotorTargetPosition = 0;


    // claw servo motor variables
    private Servo clawServo = null;
    static final double CLAW_INCREMENT = -0.004;  // amount to slew servo each CYCLE_MS cycle
    static final double CLAW_OPEN_POS = 0.25;     // Maximum rotational position
    static final double CLAW_CLOSE_POS = 0.08;
    static final double CLAW_MAX_POS = CLAW_OPEN_POS;
    static final double CLAW_MIN_POS = CLAW_CLOSE_POS;  // Minimum rotational position
    double clawServoPosition = CLAW_OPEN_POS;


    // arm servo variables
    private Servo armServo = null;
    static final double ARM_INCREMENT = 0.0015;     // amount to slew servo each CYCLE_MS cycle
    static final double ARM_MAX_POS = 0.6;     // Maximum rotational position
    static final double ARM_MIN_POS = 0.1;     // Minimum rotational position
    static final double ARM_LOAD_POSITION = 0.3;
    static final double ARM_UNLOAD_POSITION = 0.3;
    double armServoPosition = ARM_LOAD_POSITION;


    // variables for auto load and unload cone
    static final int COUNTS_PER_FEET_DRIVE = 360; // robot drive 1 feet. Back-forth moving
    static final int COUNTS_PER_FEET_STRAFE = 600; // robot strafe 1 feet. Left-right moving.
    double robotAutoLoadMovingDistance = 0.1; // in feet
    double robotAutoUnloadMovingDistance = 0.25; // in feet


    // IMU related
    Orientation lastAngles = new Orientation();
    double globalAngle = 0.0;
    double power = POWER_FACTOR; // 0.30;
    double correction = 0.0;
    double rotation = 0.0;
    PIDController pidRotate, pidDrive;
    boolean resetAngleFlag = false;
    static final int INERTIA_WAIT_TIME = 500; // in ms

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).
        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        FrontLeftDrive  = hardwareMap.get(DcMotor.class, "FrontLeft");
        FrontRightDrive = hardwareMap.get(DcMotor.class, "FrontRight");
        BackLeftDrive = hardwareMap.get(DcMotor.class,"BackLeft");
        BackRightDrive = hardwareMap.get(DcMotor.class,"BackRight");
        RightSliderMotor = hardwareMap.get(DcMotor.class,"RightSlider");
        LeftSliderMotor = hardwareMap.get(DcMotor.class,"LeftSlider");
        armServo = hardwareMap.get(Servo.class, "ArmServo");
        clawServo = hardwareMap.get(Servo.class, "ClawServo");
        imu = hardwareMap.get(BNO055IMU.class, "imu");


        // Most robots need the motor on one side to be reversed to drive forward
        // Reverse the motor that runs backwards when connected directly to the battery
        FrontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        FrontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        BackLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        BackRightDrive.setDirection(DcMotor.Direction.FORWARD);

        FrontLeftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        FrontRightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BackLeftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        BackRightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        /* slider motor control */
        // based on how Motor installed on robot.
        RightSliderMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        LeftSliderMotor.setDirection(DcMotorSimple.Direction.FORWARD);
        RightSliderMotor.setTargetPosition(sliderMotorTargetPosition);
        LeftSliderMotor.setTargetPosition(sliderMotorTargetPosition);
        // Reset slider motor encoder counts kept by the motor
        RightSliderMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        LeftSliderMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        // Set motor to run to target encoder position and top with brakes on.
        RightSliderMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        LeftSliderMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);


        // claw servo motor initial
        clawServoPosition = CLAW_OPEN_POS;
        clawServo.setPosition(clawServoPosition);

        // IMU
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.mode                = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled      = false;
        imu.initialize(parameters);

        // Set PID proportional value to start reducing power at about 50 degrees of rotation.
        // P by itself may stall before turn completed so we add a bit of I (integral) which
        // causes the PID controller to gently increase power if the turn is not completed.
        pidRotate = new PIDController(.003, .00003, 0);

        // Set PID proportional value to produce non-zero correction value when robot veers off
        // straight line. P value controls how sensitive the correction is.
        pidDrive = new PIDController(.05, 0, 0);

        // make sure the imu gyro is calibrated before continuing.
        while (!isStopRequested() && !imu.isGyroCalibrated()) {
            sleep(50);
            idle();
        }
        telemetry.addData("imu calib status", imu.getCalibrationStatus().toString());

        // Set up parameters for driving in a straight line.
        pidDrive.setSetpoint(0);
        pidDrive.setOutputRange(0, power);
        pidDrive.setInputRange(-90, 90);
        pidDrive.enable();

        // Wait for the game to start (driver presses PLAY)
        waitForStart();
        telemetry.addData("Mode", "waiting for start");
        telemetry.update();
        runtime.reset();



        // run until the end of the match (driver presses STOP)
        //while (opModeIsActive()) {
        if (opModeIsActive() && (runtime.seconds() < 30.0)) {
            // Game pad buttons design
            float robotMovingBackForth = gamepad1.left_stick_y;
            float robotMovingRightLeft = gamepad1.left_stick_x;
            float robotTurn = gamepad1.right_stick_x;
            float sliderUpDown = gamepad1.right_stick_y;
            boolean sliderGroundJunctionPosition = gamepad1.x;
            boolean sliderLowJunctionPosition = gamepad1.a;
            boolean sliderMediumJunctionPosition = gamepad1.b;
            boolean sliderHighJunctionPosition = gamepad1.y;
            boolean clawClose = gamepad1.dpad_up;
            boolean clawOpen = gamepad1.dpad_down;
            boolean armTurnLeft = gamepad1.dpad_left;
            boolean armTurnRight = gamepad1.dpad_right;
            boolean autoLoadCone = gamepad1.left_bumper;
            boolean autoUnloadCone = gamepad1.right_bumper;

            // Setup a variable for each drive wheel to save power level for telemetry
            double FrontLeftPower;
            double FrontRightPower;
            double BackLeftPower;
            double BackRightPower;


            double drive = POWER_FACTOR * Math.pow(robotMovingBackForth, 1 + (2 * RAMP_ON));
            double turn  =  POWER_FACTOR * Math.pow(-robotTurn, 1 + (2 * RAMP_ON));
            double strafe = POWER_FACTOR * Math.pow(-robotMovingRightLeft, 1 + (2 * RAMP_ON));

            // only enable correction when the turn button is not pressed.
            if (Math.abs(turn) > Math.ulp(0)) {
                pidDrive.reset();
                runtime.reset();
                resetAngleFlag = true;
                resetAngle(); // Resets the cumulative angle tracking to zero.
            }

            // turn on PID after a duration time to avoid robot inertia after turning.
            if ((runtime.milliseconds() > INERTIA_WAIT_TIME) && resetAngleFlag) {
                resetAngle(); // Resets the cumulative angle tracking to zero.
                resetAngleFlag = false;
                pidDrive.enable();
            }

            // Use PID with imu input to drive in a straight line.
            if ((Math.abs(drive) > Math.ulp(0)) || (Math.abs(strafe) > Math.ulp(0))) {
                correction = pidDrive.performPID(getAngle());
            }
            else {
                correction = 0.0;
            }
            telemetry.addData("1 imu heading (%0.2f)", lastAngles.firstAngle);
            telemetry.addData("2 global heading (%0.2f)", globalAngle);
            telemetry.addData("3 correction  (%0.2f)", correction);
            telemetry.addData("4 turn rotation", rotation);

            FrontLeftPower  = Range.clip(-drive - turn - strafe - correction, -1, 1);
            FrontRightPower = Range.clip(-drive + turn + strafe + correction, -1, 1);
            BackLeftPower   = Range.clip(-drive - turn + strafe - correction, -1, 1);
            BackRightPower  = Range.clip(-drive + turn - strafe + correction, -1, 1);


            // Send calculated power to wheels
            FrontLeftDrive.setPower(FrontLeftPower);
            FrontRightDrive.setPower(FrontRightPower);
            BackLeftDrive.setPower(BackLeftPower);
            BackRightDrive.setPower(BackRightPower);

            // use Y button to lift up the slider reaching high junction
            if (sliderHighJunctionPosition) {
                sliderMotorTargetPosition = HIGH_JUNCTION_POS;
            }

            // use B button to lift up the slider reaching medium junction
            if (sliderMediumJunctionPosition) {
                sliderMotorTargetPosition = MEDIUM_JUNCTION_POS;
            }

            // use A button to lift up the slider reaching low junction
            if (sliderLowJunctionPosition) {
                sliderMotorTargetPosition = LOW_JUNCTION_POS;
            }

            // use X button to move the slider for ground junction position
            if (sliderGroundJunctionPosition) {
                sliderMotorTargetPosition = GROUND_JUNCTION_POS;
            }

            // use right stick_Y to lift or down slider continuously
            sliderMotorTargetPosition -= (int)((sliderUpDown) * motorPositionInc);
            sliderMotorTargetPosition = Range.clip(sliderMotorTargetPosition, SLIDER_MIN_POS,
                    FOUR_STAGE_SLIDER_MAX_POS);
            telemetry.addData("Status", "slider motor Target position %d",
                    sliderMotorTargetPosition);

            RightSliderMotor.setTargetPosition(sliderMotorTargetPosition);
            LeftSliderMotor.setTargetPosition(sliderMotorTargetPosition);
            RightSliderMotor.setPower(SLIDER_MOTOR_POWER); // slider motor start movement
            LeftSliderMotor.setPower(SLIDER_MOTOR_POWER);
            telemetry.addData("Status", "Right slider motor current position %d",
                    RightSliderMotor.getCurrentPosition());
            telemetry.addData("Status", "Left slider motor current position %d",
                    LeftSliderMotor.getCurrentPosition());

            // Keep stepping up until we hit the max value.
            if (clawClose) {
                clawServoPosition += CLAW_INCREMENT;
            }
            else if (clawOpen) {
                clawServoPosition -= CLAW_INCREMENT;
            }
            clawServoPosition = Range.clip(clawServoPosition, CLAW_MIN_POS, CLAW_MAX_POS);
            clawServo.setPosition(clawServoPosition);
            telemetry.addData("Status", "Claw Servo position %.2f", clawServoPosition);

            // arm servo motor control. Keep stepping up until we hit the max value.
            if (armTurnLeft) {
                armServoPosition += ARM_INCREMENT;
            }
            else if (armTurnRight) {
                armServoPosition -= ARM_INCREMENT;
            }
            armServoPosition = Range.clip(armServoPosition, ARM_MIN_POS, ARM_MAX_POS);
            armServo.setPosition(armServoPosition);
            telemetry.addData("Status", "Arm Servo position %.2f", armServoPosition);

            //  auto driving, grip cone, and lift slider
            if(autoLoadCone) {
                autoLoadCone();
                // set arm, claw, slider position after grep.
                armServoPosition = armServo.getPosition();
                clawServoPosition = clawServo.getPosition();
                // left motor has same position with right one
                sliderMotorTargetPosition = RightSliderMotor.getCurrentPosition();
            }

            //  auto driving, unload cone
            if(autoUnloadCone) {
                autoUnloadCone();
                // set arm, claw, slider position after grep.
                armServoPosition = armServo.getPosition();
                clawServoPosition = clawServo.getPosition();
                sliderMotorTargetPosition = RightSliderMotor.getCurrentPosition();
            }

            // Show the elapsed game time and wheel power, positions.
            telemetry.addData("Motors", "Frontleft (%.2f), Frontright (%.2f)," +
                            " Backleft (%.2f), Backright (%.2f)", FrontLeftPower, FrontRightPower,
                    BackLeftPower,BackRightPower);

            telemetry.addData("Motors Positions:",
                    "Frontleft (%d), Frontright (%d)," + " Backleft (%d), Backright (%d)",
                    FrontLeftDrive.getCurrentPosition(), FrontRightDrive.getCurrentPosition(),
                    BackLeftDrive.getCurrentPosition(), BackRightDrive.getCurrentPosition());


            /** code for autonomous
             * 1. take a picture, recognize the color on sleeve signal
             * 2. Move robot the high junction
             * 3. Unload cone on high junction
             * 4. Move robot to cone loading area
             * 5. Load cone
             * 6. Move robot to parking area
             */
            int sleevSignal = 1; // sleeve signal will be 1 for red, 2 for green, 3 for blue
            double parkingLocation = 1.0; // distance between cone loading area to parking area
            switch (sleevSignal) {
                case 1:
                    parkingLocation = 5.0; // parking lot #1 (red), third mat
                    break;
                case 2:
                    parkingLocation = 3.0; // parking lot #2 (green), third mat
                    break;
                case 3:
                    parkingLocation = 1.0; // parking lot #3 (blue), third mat
                    break;
                default:
                    parkingLocation = 0.0;
            }
            telemetry.addData("Status", "auto mode - sleeve signal (%d)," +
                    "moving distance (%0.1f) feet", sleevSignal, parkingLocation);

            robotMovingDistance(5.0, true); // drive robot to the center of 3rd mat
            robotMovingDistance(-1.0, false); // strafe robot half mat to left side
            robotMovingDistance(1.0, true); // drive robot half mat to high junction
            autoUnloadCone();
            rotate(90, AUTO_DRIVE_POWER); // turn robot 90 degree to right
            robotMovingDistance(3.5, true); // drive robot to loading area
            autoLoadCone(); // need update to input cone height position
            rotate(180, AUTO_DRIVE_POWER); // turn robot 90 degree to right
            robotMovingDistance(parkingLocation, true); // drive robot to parking
            robotMovingDistance(5.0, false); // strafe robot to parking mat

            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.update(); // update message at the end of while loop
        }

        // The motor stop on their own but power is still applied. Turn off motor.
        RightSliderMotor.setPower(0.0);
        LeftSliderMotor.setPower(0.0);
    }

    /**
     * 1. Robot moving back to aim at junction for unloading cone
     * 2. Slider moving down a little bit to put cone in junction pole
     * 3. Open claw to fall down cone
     * 4. Lift slider from junction pole
     * 5. Robot moving back to leave junction
     * 6. Slider moving down to get ready to grip another cone
     */
    private void autoUnloadCone() {
        armServo.setPosition(ARM_UNLOAD_POSITION);
        robotMovingDistance(-robotAutoUnloadMovingDistance, true); // moving back in feet

        // move down slider a little bit to unload cone
        sliderMotorTargetPosition = RightSliderMotor.getCurrentPosition();
        int moveSlider = sliderMotorTargetPosition - SLIDER_MOVE_DOWN_POSITION;
        moveSlider = Math.max(moveSlider, SLIDER_MIN_POS);
        RightSliderMotor.setTargetPosition(moveSlider);
        LeftSliderMotor.setTargetPosition(moveSlider);
        waitMotorActionComplete(RightSliderMotor); // make sure left and right motor are complete actions
        waitMotorActionComplete(LeftSliderMotor);

        clawServo.setPosition(CLAW_OPEN_POS); // unload  cone
        sleep(400); // wait 0.4 sec to make sure clawServo is at grep position

        sliderMotorTargetPosition = RightSliderMotor.getCurrentPosition();
        moveSlider = sliderMotorTargetPosition + SLIDER_MOVE_DOWN_POSITION;
        RightSliderMotor.setTargetPosition(moveSlider);
        LeftSliderMotor.setTargetPosition(moveSlider);
        waitMotorActionComplete(RightSliderMotor);
        waitMotorActionComplete(LeftSliderMotor);
        robotMovingDistance(-robotAutoUnloadMovingDistance, true); // move out from junction
        armServo.setPosition(ARM_LOAD_POSITION);
        RightSliderMotor.setTargetPosition(READY_FOR_GRIP_POSITION);
        LeftSliderMotor.setTargetPosition(READY_FOR_GRIP_POSITION);
        waitMotorActionComplete(RightSliderMotor);
        waitMotorActionComplete(LeftSliderMotor);
    }

    /**
     * 1. Lift slider and open claw to get read to load a cone
     * 2. Robot moving back to aim at cone for loading
     * 2. Slider moving down to load the cone
     * 3. Close claw to grip the cone
     * 4. Lift slider to low junction position for unloading
     * 5. Robot moving back to leave junction
     * 6. Slider moving down to get ready to grip another cone
     */
    private void autoLoadCone() {
        RightSliderMotor.setTargetPosition(READY_FOR_GRIP_POSITION);
        LeftSliderMotor.setTargetPosition(READY_FOR_GRIP_POSITION);
        clawServo.setPosition(CLAW_OPEN_POS);
        armServo.setPosition(ARM_LOAD_POSITION);
        RightSliderMotor.setPower(SLIDER_MOTOR_POWER); // slider motor starts movement
        LeftSliderMotor.setPower(SLIDER_MOTOR_POWER);
        robotMovingDistance(-robotAutoLoadMovingDistance, true); // moving to loading position
        waitMotorActionComplete(RightSliderMotor);
        waitMotorActionComplete(LeftSliderMotor);
        RightSliderMotor.setTargetPosition(SLIDER_MIN_POS);
        LeftSliderMotor.setTargetPosition(SLIDER_MIN_POS);
        waitMotorActionComplete(RightSliderMotor);
        waitMotorActionComplete(LeftSliderMotor);
        clawServo.setPosition(CLAW_CLOSE_POS);
        sleep(400); // wait 0.4 sec to make sure clawServo is at grep position
        armServo.setPosition(ARM_UNLOAD_POSITION);
        RightSliderMotor.setTargetPosition(LOW_JUNCTION_POS);
        LeftSliderMotor.setTargetPosition(LOW_JUNCTION_POS);
        waitMotorActionComplete(RightSliderMotor);
        waitMotorActionComplete(LeftSliderMotor);
    }

    /**
     * Set target position for every wheel motor, and set power to motors to move the robot.
     * Turn off encode mode after moving.
     * @param targetDistance: Input value for the target distance in feet.
     * @param isBackForth: flag for back-forth (true) moving, or left-right moving (false)
     */
    private void robotMovingDistance(double targetDistance, boolean isBackForth) {
        int countsPerFeet = isBackForth? COUNTS_PER_FEET_DRIVE : COUNTS_PER_FEET_STRAFE;
        int targetPosition = (int)(targetDistance * countsPerFeet);
        telemetry.addData("Status", "auto driving target position %d", targetPosition);
        setTargetPositionsToWheels(targetPosition, isBackForth);
        robotRunWithPositionModeOn(true); // turn on encoder mode
        //setPowerToWheels(AUTO_DRIVE_POWER); // low speed for more accurate, start moving
        //waitMotorActionComplete(FrontLeftDrive); // just check one wheel.
        robotDriveWithPIDControl(AUTO_DRIVE_POWER);
        //setPowerToWheels(0.0); //stop moving
        robotRunWithPositionModeOn(false); // turn off encoder mode
    }

    /**
     * Set wheels motors to stop and reset encode to set the current encoder position to zero.
     * And then set to run to position mode if withPositionMode is on.
     * Otherwise, set to run without encode mode.
     * @param withPositionMode: flag for wheels motors run with position mode on,
     *                       or off(run without encode)
     */
    private void robotRunWithPositionModeOn(boolean withPositionMode) {
        if (withPositionMode) {
            FrontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            FrontLeftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            FrontRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            FrontRightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            BackLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            BackLeftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            BackRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            BackRightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        else {
            // set back to WITHOUT ENCODER mode
            FrontLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            FrontRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            BackLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            BackRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    /**
     * Set wheels motors target positions according to back-forward moving flag
     * @param tPos: target position values for motors
     * @param isBF: flag for back-forward moving or left-right moving.
     *            Back forward(1), or left right (0)
     */
    private void setTargetPositionsToWheels(int tPos, boolean isBF) {
        if (isBF) {
            FrontLeftDrive.setTargetPosition( tPos );
            FrontRightDrive.setTargetPosition( tPos );
            BackLeftDrive.setTargetPosition( tPos );
            BackRightDrive.setTargetPosition( tPos );
        }
        else {// move left or right, positive for right
            FrontLeftDrive.setTargetPosition( tPos );
            FrontRightDrive.setTargetPosition( -tPos );
            BackLeftDrive.setTargetPosition( -tPos );
            BackRightDrive.setTargetPosition( tPos );
        }
    }

    /**
     * Set wheels motors power
     * @param power: the power value set to motors (0.0 ~ 1.0)
     */
    private void setPowerToWheels(double power) {
        FrontLeftDrive.setPower(power);
        FrontRightDrive.setPower(power);
        BackLeftDrive.setPower(power);
        BackRightDrive.setPower(power);
    }

    /**
     * Wait until the motor complete action.
     * The MAXIMUM waiting time is MAX_WAIT_TIME to avoid death.
     * @param mot: the motor which be checked if it is in active.
     */
    private void waitMotorActionComplete(DcMotor mot) {
        double curTime = runtime.seconds();
        while((mot.isBusy()) && ((runtime.seconds() - curTime) < MAX_WAIT_TIME)) {
            idle();
        }
    }


    /**
     * Resets the cumulative angle tracking to zero.
     */
    private void resetAngle() {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        globalAngle = 0;
    }

    /**
     * Get current cumulative angle rotation from last reset.
     * @return Angle in degrees. + = left, - = right from zero point.
     */
    private double getAngle() {
        // We experimentally determined the Z axis is the axis we want to use for heading angle.
        // We have to process the angle because the imu works in euler angles so the Z axis is
        // returned as 0 to +180 or 0 to -180 rolling back to -179 or +179 when rotation passes
        // 180 degrees. We detect this transition and track the total cumulative angle of rotation.

        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        globalAngle += deltaAngle;

        lastAngles = angles;

        return globalAngle;
    }

    /**
     * Rotate left or right the number of degrees. Does not support turning more than 359 degrees.
     * @param degrees Degrees to turn, + is left - is right
     */
    private void rotate(int degrees, double power) {
        // restart imu angle tracking.
        resetAngle();

        // if degrees > 359 we cap at 359 with same sign as original degrees.
        if (Math.abs(degrees) > 359)
            degrees = (int) Math.copySign(359, degrees);

        // start pid controller. PID controller will monitor the turn angle with respect to the
        // target angle and reduce power as we approach the target angle. This is to prevent the
        // robots momentum from overshooting the turn after we turn off the power. The PID controller
        // reports onTarget() = true when the difference between turn angle and target angle is within
        // 1% of target (tolerance) which is about 1 degree. This helps prevent overshoot. Overshoot is
        // dependant on the motor and gearing configuration, starting power, weight of the robot and the
        // on target tolerance. If the controller overshoots, it will reverse the sign of the output
        // turning the robot back toward the setpoint value.

        pidRotate.reset();
        pidRotate.setSetpoint(degrees);
        pidRotate.setInputRange(0, degrees);
        pidRotate.setOutputRange(0, power);
        pidRotate.setTolerance(1);
        pidRotate.enable();

        // getAngle() returns + when rotating counter clockwise (left) and - when rotating
        // clockwise (right).

        // rotate until turn is completed.

        if (degrees < 0) {
            // On right turn we have to get off zero first.
            while (opModeIsActive() && getAngle() == 0) {
                leftMotorSetPower(power);
                rightMotorSetPower(-power);
                sleep(100);
            }

            do {
                power = pidRotate.performPID(getAngle()); // power will be - on right turn.
                leftMotorSetPower(-power);
                rightMotorSetPower(power);
            } while (opModeIsActive() && !pidRotate.onTarget());
        }
        else    // left turn.
            do {
                power = pidRotate.performPID(getAngle()); // power will be + on left turn.
                leftMotorSetPower(-power);
                rightMotorSetPower(power);
            } while (opModeIsActive() && !pidRotate.onTarget());

        // turn the motors off.
        rightMotorSetPower(0);
        leftMotorSetPower(0);

        rotation = getAngle();

        // wait for rotation to stop.
        sleep(500);

        // reset angle tracking on new heading.
        resetAngle();
    }

    /**
     * Set left side motors power.
     * @param p the power set to front left motor and back left motor
     */
    private void leftMotorSetPower(double p) {
        FrontLeftDrive.setPower(p);
        BackLeftDrive.setPower(p);
    }

    /**
     * Set right side motors power.
     * @param p the power set to front left right motor and back right motor
     */
    private void rightMotorSetPower(double p) {
        FrontRightDrive.setPower(p);
        BackRightDrive.setPower(p);
    }

    /**
     * Set motors power and drive or strafe robot straightly with run_to_position mode by PID control.
     * @param p the power set to the robot motors
     */
    private void robotDriveWithPIDControl(double p) {
        double curTime = runtime.seconds();
        correction = 0.0;
        setPowerToWheels(p);
        while(robotIsBusy() && ((runtime.seconds() - curTime) < MAX_WAIT_TIME)) {
            correction = pidDrive.performPID(getAngle());
            leftMotorSetPower(p - correction);
            rightMotorSetPower(p + correction);
            sleep(100);
        }
        setPowerToWheels(0.0); //stop moving
    }

    /**
     * Check if robot motors are busy. Return ture if yes, false otherwise.
     */
    private boolean robotIsBusy() {
        return (FrontRightDrive.isBusy() || FrontLeftDrive.isBusy() ||
                BackLeftDrive.isBusy() || BackRightDrive.isBusy());
    }
}
