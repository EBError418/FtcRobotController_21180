/* Copyright (c) 2019 FIRST. All rights reserved.
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

package org.firstinspires.ftc.teamcode.Reno.poc;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XZY;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.teamcode.Reno.FieldFloor;
import org.firstinspires.ftc.teamcode.Reno.FieldTile;
import org.firstinspires.ftc.teamcode.Reno.HardwareRobot;

import java.util.ArrayList;
import java.util.List;

/**
 * This OpMode illustrates using the Vuforia localizer to determine positioning and orientation of
 * robot on the FTC field using a WEBCAM.  The code is structured as a LinearOpMode
 *
 * NOTE: If you are running on a Phone with a built-in camera, use the ConceptVuforiaFieldNavigation example instead of this one.
 * NOTE: It is possible to switch between multiple WebCams (eg: one for the left side and one for the right).
 *       For a related example of how to do this, see ConceptTensorFlowObjectDetectionSwitchableCameras
 *
 * When images are located, Vuforia is able to determine the position and orientation of the
 * image relative to the camera.  This sample code then combines that information with a
 * knowledge of where the target images are on the field, to determine the location of the camera.
 *
 * Finally, the location of the camera on the robot is used to determine the
 * robot's location and orientation on the field.
 *
 * To learn more about the FTC field coordinate model, see FTC_FieldCoordinateSystemDefinition.pdf in this folder
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this OpMode to the Driver Station OpMode list.
 *
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */

@Autonomous(name="POC - Field Nav Encoder", group ="Concept")
@Disabled
public class ConceptFieldNavigationAutoV2 extends LinearOpMode {

    HardwareRobot robot   = new HardwareRobot();   // Use a Pushbot's hardware
    private ElapsedTime     runtime = new ElapsedTime();

    double power = 0.2;

    FieldFloor floor = new FieldFloor();

    /*
     * IMPORTANT: You need to obtain your own license key to use Vuforia. The string below with which
     * 'parameters.vuforiaLicenseKey' is initialized is for illustration only, and will not function.
     * A Vuforia 'Development' license key, can be obtained free of charge from the Vuforia developer
     * web site at https://developer.vuforia.com/license-manager.
     *
     * Vuforia license keys are always 380 characters long, and look as if they contain mostly
     * random data. As an example, here is a example of a fragment of a valid key:
     *      ... yIgIzTqZ4mWjk9wd3cZO9T1axEqzuhxoGlfOOI2dRzKS4T0hQ8kT ...
     * Once you've obtained a license key, copy the string from the Vuforia web site
     * and paste it in to your code on the next line, between the double quotes.
     */
    private static final String VUFORIA_KEY =
            "AQFuTJD/////AAABmRQf1MrshUsMqfZFoea5LcBlQ79rSmu2Hsy0VUgvFAIXZWBCbzYDL8mVWF1K4NKNgTmlUGl" +
                    "mL6QrKaxd+oCP2VfTB5U+hgJ6KfZ5y2b6eV05AXaI/YWSyXQSdU/U7sKj+DuHMLbbK8wROJaJhoCoFz3" +
                    "HVWr9wknksiGASbHmm3B60ItfxXUm4oKrkBQGhVwOFfejn/Cq2tticMWg0Vv8fy/v4O2nf0vgLIHV5unW5" +
                    "CpuKiTfKAZqkfHgHYcB693Ebnkx3ZOBJy1Vv74xk1BSRRoR1HIjqQINhUWhws/yoHKsgq1z7COmzOzoh" +
                    "0WFj5jmLgjKDslJccFmcemOOYdejUA8u4SIARjiErm6Ow+kfvuU";

    // Since ImageTarget trackables use mm to specifiy their dimensions, we must use mm for all the physical dimension.
    // We will define some constants and conversions here
    private static final float mmPerInch        = 25.4f;
    private static final float mmTargetHeight   = 6 * mmPerInch;          // the height of the center of the target image above the floor
    private static final float halfField        = 72 * mmPerInch;
    private static final float halfTile         = 12 * mmPerInch;
    private static final float oneAndHalfTile   = 36 * mmPerInch;

    // Class Members
    private OpenGLMatrix lastLocation   = null;
    private VuforiaLocalizer vuforia    = null;
    private VuforiaTrackables targets   = null ;
    private WebcamName webcamName       = null;

    private boolean targetVisible       = false;

    private List<VuforiaTrackable> allTrackables = new ArrayList<VuforiaTrackable>();
    final float CAMERA_FORWARD_DISPLACEMENT  = 0.0f * mmPerInch;   // eg: Enter the forward distance from the center of the robot to the camera lens
    final float CAMERA_VERTICAL_DISPLACEMENT = 6.0f * mmPerInch;   // eg: Camera is 6 Inches above ground
    final float CAMERA_LEFT_DISPLACEMENT     = 0.0f * mmPerInch;   // eg: Enter the left distance from the center of the robot to the camera lens

    static final double     COUNTS_PER_MOTOR_REV    = 1440 ;    // eg: TETRIX Motor Encoder
    static final double     DRIVE_GEAR_REDUCTION    = 1.0 ;     // No External Gearing.
    static final double     WHEEL_DIAMETER_INCHES   = 4.0 ;     // For figuring circumference
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.1415);
    static final double     DRIVE_SPEED             = 0.6;
    static final double     TURN_SPEED              = 0.5;
    public void initWebCamera() {

        webcamName = hardwareMap.get(WebcamName.class, "Webcam 1");
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
        // VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;

        // We also indicate which camera we wish to use.
        parameters.cameraName = webcamName;

        // Turn off Extended tracking.  Set this true if you want Vuforia to track beyond the target.
        parameters.useExtendedTracking = false;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Load the data sets for the trackable objects. These particular data
        // sets are stored in the 'assets' part of our application.
        targets = this.vuforia.loadTrackablesFromAsset("PowerPlay");
        allTrackables.addAll(targets);

        identifyTarget(0, "Red Audience Wall",   -halfField,  -oneAndHalfTile, mmTargetHeight, 90, 0,  90);
        identifyTarget(1, "Red Rear Wall",        halfField,  -oneAndHalfTile, mmTargetHeight, 90, 0, -90);
        identifyTarget(2, "Blue Audience Wall",  -halfField,   oneAndHalfTile, mmTargetHeight, 90, 0,  90);
        identifyTarget(3, "Blue Rear Wall",       halfField,   oneAndHalfTile, mmTargetHeight, 90, 0, -90);

        OpenGLMatrix cameraLocationOnRobot = OpenGLMatrix
                .translation(CAMERA_FORWARD_DISPLACEMENT, CAMERA_LEFT_DISPLACEMENT, CAMERA_VERTICAL_DISPLACEMENT)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XZY, DEGREES, 90, 90, 0));

        /**  Let all the trackable listeners know where the camera is.  */
        for (VuforiaTrackable trackable : allTrackables) {
            ((VuforiaTrackableDefaultListener) trackable.getListener()).setCameraLocationOnRobot(parameters.cameraName, cameraLocationOnRobot);
        }

        targets.activate();

    }
    public void findLocation()
    {
        if (opModeIsActive())
        {

            // check all the trackable targets to see which one (if any) is visible.
            targetVisible = false;
            for (VuforiaTrackable trackable : allTrackables)
            {
                if (((VuforiaTrackableDefaultListener) trackable.getListener()).isVisible())
                {
                    telemetry.addData("Visible Target", trackable.getName());
                    targetVisible = true;

                    // getUpdatedRobotLocation() will return null if no new information is available since
                    // the last time that call was made, or if the trackable is not currently visible.
                    OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener) trackable.getListener()).getUpdatedRobotLocation();
                    if (robotLocationTransform != null) {
                        lastLocation = robotLocationTransform;
                    }
                    break;
                }
            }
        }
    }

    public void encoderDrive(double speed,
                             double leftInches, double rightInches,
                             double timeoutS) {
        int newLeftTarget;
        int newRightTarget;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            newLeftTarget = robot.leftDriveFront.getCurrentPosition() + (int)(leftInches * COUNTS_PER_INCH);
            newRightTarget = robot.rightDriveFront.getCurrentPosition() + (int)(rightInches * COUNTS_PER_INCH);
            robot.leftDriveFront.setTargetPosition(newLeftTarget);
            robot.rightDriveFront.setTargetPosition(newRightTarget);

            // Turn On RUN_TO_POSITION
            robot.leftDriveFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.rightDriveFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // reset the timeout time and start motion.
            runtime.reset();
            robot.leftDriveFront.setPower(Math.abs(speed));
            robot.rightDriveFront.setPower(Math.abs(speed));

            // keep looping while we are still active, and there is time left, and both motors are running.
            // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
            // its target position, the motion will stop.  This is "safer" in the event that the robot will
            // always end the motion as soon as possible.
            // However, if you require that BOTH motors have finished their moves before the robot continues
            // onto the next step, use (isBusy() || isBusy()) in the loop test.
            while (opModeIsActive() &&
                    (runtime.seconds() < timeoutS) &&
                    (robot.leftDriveFront.isBusy() && robot.rightDriveFront.isBusy())) {

                // Display it for the driver.
                telemetry.addData("Running to",  " %7d :%7d", newLeftTarget,  newRightTarget);
                telemetry.addData("Currently at",  " at %7d :%7d",
                        robot.leftDriveFront.getCurrentPosition(), robot.rightDriveFront.getCurrentPosition());
                telemetry.update();
            }

            // Stop all motion;
            robot.leftDriveFront.setPower(0);
            robot.rightDriveFront.setPower(0);

            // Turn off RUN_TO_POSITION
            robot.leftDriveFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.rightDriveFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            sleep(250);   // optional pause after each move.
        }
    }
    @Override public void runOpMode() {

        this.initWebCamera();
        
        robot.init(hardwareMap);


        telemetry.addData("Status", "Ready to run");    //
        telemetry.update();

        robot.leftDriveFront.setDirection(DcMotorSimple.Direction.REVERSE);
        robot.leftDriveBack.setDirection(DcMotorSimple.Direction.REVERSE);

        robot.leftDriveFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightDriveFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        robot.leftDriveFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.rightDriveFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

         waitForStart();

        runtime.reset();


        /* Note: To use the remote camera preview:
         * AFTER you hit Init on the Driver Station, use the "options menu" to select "Camera Stream"
         * Tap the preview window to receive a fresh image.
         * It is not permitted to transition to RUN while the camera preview window is active.
         * Either press STOP to exit the OpMode, or use the "options menu" again, and select "Camera Stream" to close the preview window.
         */


        while (opModeIsActive() && (runtime.seconds() < 30.0))
        {

            // check all the trackable targets to see which one (if any) is visible.
            targetVisible = false;

            this.findLocation();

            // Provide feedback as to where the robot is located (if we know).
            if (targetVisible) {
                // express position (translation) of robot in inches.
                VectorF translation = lastLocation.getTranslation();
                telemetry.addData("Pos (inches)", "{X, Y, Z} = %.1f, %.1f, %.1f",
                        translation.get(0) / mmPerInch, translation.get(1) / mmPerInch, translation.get(2) / mmPerInch);

                // express the rotation of the robot in degrees.
                Orientation rotation = Orientation.getOrientation(lastLocation, EXTRINSIC, XYZ, DEGREES);
                telemetry.addData("Rot (deg)", "{Roll, Pitch, Heading} = %.0f, %.0f, %.0f", rotation.firstAngle, rotation.secondAngle, rotation.thirdAngle);

                telemetry.update();
                sleep(2000);
                double robotStartingX = translation.get(0);// / mmPerInch; // Robot X axis
                double robotStartingY = translation.get(1);// / mmPerInch; // Robot Y axis
                double robotStartingZ = translation.get(2);// / mmPerInch; // Robot Y axis

                for (int x = 0; x < floor.matrix.length; x++)
                {
                    for (int y = 0; y < floor.matrix[x].length; y++)
                    {
                        FieldTile tile = floor.matrix[x][y];

                        double distanceX = tile.destX - robotStartingX;
                        double distanceY = tile.destY - robotStartingY;

                        encoderDrive(DRIVE_SPEED,  distanceX,  distanceX, 5.0);  // S1: Forward 47 Inches with 5 Sec timeout
                        encoderDrive(TURN_SPEED,   12, -12, 4.0);  // S2: Turn Right 12 Inches with 4 Sec timeout
                        encoderDrive(DRIVE_SPEED, distanceY, distanceY, 4.0);  // S3: Reverse 24 Inches with 4 Sec timeout

                        robotStartingX = tile.destX;
                        robotStartingY = tile.destY;
                    }
                }



            }
            else
            {
                telemetry.addData("Visible Target", "none");
                robot.turn(-0.2);
                sleep(1000);
            }
            telemetry.update();
        }

        // Disable Tracking when we are done;
        targets.deactivate();
    }

    /***
     * Identify a target by naming it, and setting its position and orientation on the field
     * @param targetIndex
     * @param targetName
     * @param dx, dy, dz  Target offsets in x,y,z axes
     * @param rx, ry, rz  Target rotations in x,y,z axes
     */
    void    identifyTarget(int targetIndex, String targetName, float dx, float dy, float dz, float rx, float ry, float rz) {
        VuforiaTrackable aTarget = targets.get(targetIndex);
        aTarget.setName(targetName);
        aTarget.setLocation(OpenGLMatrix.translation(dx, dy, dz)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, rx, ry, rz)));
    }


}
