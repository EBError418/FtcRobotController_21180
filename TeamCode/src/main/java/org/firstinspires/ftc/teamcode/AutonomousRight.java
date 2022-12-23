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

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

import java.util.List;

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

@Autonomous(name="Autonomous_Right", group="Concept")
//@Disabled
public class AutonomousRight extends LinearOpMode {

    public int autonomousStartLocation = 1; // 1 for right location, and -1 for left location.

    // Declare OpMode members.
    private final ElapsedTime runtime = new ElapsedTime();
    public final ChassisWith4Motors chassis = new ChassisWith4Motors();
    private final SlidersWith2Motors slider = new SlidersWith2Motors();
    private final ArmClawUnit armClaw = new ArmClawUnit();

    // variables for autonomous
    double robotAutoLoadMovingDistance = 1.0; // in INCH
    double matCenterToJunctionDistance = 14.5;
    double movingDistBeforeDrop = 3.5; // in INCH
    double movingDistAfterDrop = matCenterToJunctionDistance - movingDistBeforeDrop - 3.2; // 2.8 INCH for inertia adjust
    double matCenterToConeStack = 28; // inch
    double moveToMatCenterAfterPick = matCenterToConeStack - robotAutoLoadMovingDistance - 2; // 1 inch for inertia adjust

    // camera and sleeve color
    ObjectDetection.ParkingLot myParkingLot = ObjectDetection.ParkingLot.UNKNOWN;
    double parkingLotDis = 0;
    ObjectDetection coneSleeveDetect;
    OpenCvCamera camera;
    String webcamName = "Webcam 1";
    boolean isCameraInstalled = true;

    // variables for location shift
    double[] xyShift = {0.0, 0.0};

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        Logging.log("Status - Initialized");

        setRobotLocation();

        // camera for sleeve color detect, start camera at the beginning.
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());

        coneSleeveDetect = new ObjectDetection();

        if (isCameraInstalled) {
            camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(
                    WebcamName.class, webcamName), cameraMonitorViewId);

            camera.setPipeline(coneSleeveDetect);

            camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                @Override
                public void onOpened() {
                    camera.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
                    Logging.log("Start stream to detect sleeve color.");
                    telemetry.addData("Start stream to detect sleeve color.", "ok");
                    telemetry.update();
                }

                @Override
                public void onError(int errorCode) {
                    Logging.log("Start stream error.");
                    telemetry.addData("Start stream to detect sleeve color.", "error");
                    telemetry.update();
                }
            });
        }

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).
        slider.init(hardwareMap, "RightSlider", "LeftSlider");
        chassis.init(hardwareMap, "FrontLeft", "FrontRight",
                "BackLeft", "BackRight");

        armClaw.init(hardwareMap, "ArmServo", "ClawServo");
        armClaw.clawClose();

        runtime.reset();
        while ((ObjectDetection.ParkingLot.UNKNOWN == myParkingLot) &&
                ((runtime.seconds()) < 3.0)) {
            myParkingLot = coneSleeveDetect.getParkingLot();
        }
        Logging.log("Parking Lot position: %s", myParkingLot.toString());

        while (!isStarted()) {
            myParkingLot = coneSleeveDetect.getParkingLot();
            parkingLotDis = coneSleeveDetect.getParkingLotDistance();
            telemetry.addData("Parking position: ", myParkingLot);
            telemetry.addData("robot position: ", autonomousStartLocation > 0? "Right":"Left");
            telemetry.addData("Mode", "waiting for start");
            telemetry.update();
        }

        // bulk reading setting - auto refresh mode
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        // Wait for the game to start (driver presses PLAY)
        waitForStart();
        runtime.reset();


        // run until the end of the match (driver presses STOP)
        if (opModeIsActive()) {
            camera.stopRecordingPipeline();
            camera.closeCameraDevice();

            autonomousCore();

            Logging.log("Autonomous - total Run Time: " + runtime);
            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.update(); // update message at the end of loop
        }

        // The motor stop on their own but power is still applied. Turn off motor.
        slider.stop();
        chassis.setPowers(0.0);
    }

    /** code for autonomous
     * 1. take a picture, recognize the color on sleeve signal
     * 2. Move robot the high junction
     * 3. Unload cone on high junction
     * 4. Move robot to cone loading area
     * 5. Load cone
     * 6. Move robot to parking area
     */
    public void autonomousCore() {

        slider.setPower(slider.SLIDER_MOTOR_POWER);
        slider.setPosition(FieldParams.LOW_JUNCTION_POS);

        //move center of robot to the edge of 3rd mat
        chassis.runToPosition(65.5, true);

        // turn robot to make sure it is at 0 degree before backing to mat center
        chassis.rotateIMUTargetAngle(0.0);

        // driving back to mat center
        chassis.runToPosition(-14, true);

        // lift slider during rotating robot 45 degrees left
        slider.setPosition(FieldParams.HIGH_JUNCTION_POS);
        chassis.rotateIMUTargetAngle(45.0 * autonomousStartLocation);
        Logging.log("fcDistance sensor value before moving V to junction: %.2f ", chassis.getFcDSValue());
        slider.waitRunningComplete();

        //drive forward and let V to touch junction
        chassis.runToPosition(matCenterToJunctionDistance, true);
        Logging.log("Autonomous - Robot V reached junction.");

        // drop cone and back to the center of mat
        autoUnloadCone(movingDistBeforeDrop + 0.5, movingDistAfterDrop);

        for(int autoLoop = 0; autoLoop < 2; autoLoop++) {
            Logging.log("Autonomous - loop index: %d ", autoLoop);

            // right turn 135 degree (45 + 90).
            chassis.rotateIMUTargetAngle(-90.0 * autonomousStartLocation);
            sleep(100); // wait for chassis stop from previous rotation inertia.

            // strafe to left/right a little bit to compensate for the shift from previous rotation.
            chassis.runToPosition(xyShift[0], false);

            // Rotation for accurate 135 degrees
            chassis.rotateIMUTargetAngle(-90.0 * autonomousStartLocation);

            // drive robot to loading area. xyShift is according to the testing from 135 degrees turning.
            chassis.runToPosition(matCenterToConeStack - xyShift[1], true);
            Logging.log("Autonomous - Robot has arrived loading area.");

            Logging.log("fcDistance sensor value before loading: %.2f ", chassis.getFcDSValue());
            // load cone
            autoLoadCone(FieldParams.coneStack5th - FieldParams.coneLoadStackGap * autoLoop);

            // lift cone and make sure robot is in the same orientation before back to junction
            slider.setPosition(FieldParams.WALL_POSITION);
            chassis.rotateIMUTargetAngle(-90.0 * autonomousStartLocation);
            slider.waitRunningComplete(); // make sure slider has been lifted.

            // lift slider during driving back to mat center.
            slider.setPosition(FieldParams.MEDIUM_JUNCTION_POS);
            chassis.runToPosition(-moveToMatCenterAfterPick, true);
            Logging.log("Autonomous - Robot arrived the mat center near high junction.");

            // lift slider during left turning 135 degree facing to high junction.
            slider.setPosition(FieldParams.HIGH_JUNCTION_POS);
            chassis.rotateIMUTargetAngle(45.0 * autonomousStartLocation);
            sleep(100); // wait for chassis stop from previous rotation inertia.

            //Rotation for accurate 45 degrees
            chassis.rotateIMUTargetAngle(45.0 * autonomousStartLocation);

            // adjust xy shift.
            chassis.runToPosition(-xyShift[0], false);

            // make sure slider has been lifted
            slider.waitRunningComplete();
            Logging.log("Autonomous - slider is positioned to high junction.");

            // moving forward V to junction
            chassis.runToPosition(matCenterToJunctionDistance + xyShift[1], true);

            // Make sure it is 45 degree before V leaving junction
            chassis.rotateIMUTargetAngle(45.0 * autonomousStartLocation);

            // unload cone & adjust, 0.2 inch for cone thickness adjust
            autoUnloadCone(movingDistBeforeDrop - 0.2, movingDistAfterDrop);
            Logging.log("Autonomous - cone %d has been unloaded.", autoLoop + 2);
        }

        //rotate 45 degrees to keep orientation at 90
        chassis.rotateIMUTargetAngle(-90.0 * autonomousStartLocation);

        // lower slider in prep for tele-op
        slider.setPosition(FieldParams.GROUND_CONE_POSITION);

        // drive to final parking lot
        chassis.runToPosition(parkingLotDis * autonomousStartLocation, true);
        Logging.log("Autonomous - Arrived at parking lot Mat: %.2f", parkingLotDis);

        slider.waitRunningComplete();
        Logging.log("Autonomous - Autonomous complete.");
    }

    /**
     * During autonomous, cone may be located with different height position
     * 1. Lift slider and open claw to get read to load a cone
     * 2. Robot moving back to aim at cone for loading
     * 2. Slider moving down to load the cone
     * 3. Close claw to grip the cone
     * 4. Lift slider to low junction position for unloading
     * 5. Robot moving back to leave junction
     * 6. Slider moving down to get ready to grip another cone
     * @param coneLocation: the target cone high location.
     */
    private void autoLoadCone(double coneLocation) {
        armClaw.clawOpen();
        slider.setPosition(coneLocation);
        chassis.runToPosition(-robotAutoLoadMovingDistance, true); // back a little bit to avoid stuck.
        slider.waitRunningComplete();
        armClaw.clawClose();
        Logging.log("Auto load - Cone has been loaded.");
        sleep(200); // wait to make sure clawServo is at grep position
    }

    /**
     * 1. Robot moving back to aim at junction for unloading cone
     * 2. Slider moving down a little bit to put cone in junction pole
     * 3. Open claw to fall down cone
     * 4. Lift slider from junction pole during the robot moving back to leave junction
     * 6. Slider moving down to get ready to grip another cone
     * @param moveDistanceBeforeDrop moving back distance before opening claw to drop cone.
     * @param moveDistanceAfterDrop moving back distance after dropping the cone to move out claw from junction.
     */
    private void autoUnloadCone(double moveDistanceBeforeDrop, double moveDistanceAfterDrop) {
        // moving back in inch
        chassis.runToPosition(-moveDistanceBeforeDrop, true);

        // move down slider a little bit to unload cone
        double sliderInchPos = slider.getPosition() / (double)slider.COUNTS_PER_INCH;
        sleep(800); // wait for avoiding junction shaking
        slider.setPosition(sliderInchPos - FieldParams.SLIDER_MOVE_DOWN_POSITION);
        slider.waitRunningComplete();

        armClaw.clawOpen();// unload cone
        sleep(100); // make sure cone has been unloaded
        slider.setPosition(sliderInchPos);
        //chassis.runToPosition(-moveDistanceAfterDrop, true); // move out from junction
        chassis.runWithEncoderAndDistanceSensor(14, -moveDistanceAfterDrop, 2, 0.2);
        Logging.log("fcDistance sensor value after unloading: %.2f ", chassis.getFcDSValue());
        slider.setPosition(FieldParams.WALL_POSITION);
        Logging.log("Auto unload - Cone has been unloaded.");
    }

/**
 * Set robot starting position: 1 for right and -1 for left.
 */
    public void setRobotLocation() {
        autonomousStartLocation = 1;
    }

}