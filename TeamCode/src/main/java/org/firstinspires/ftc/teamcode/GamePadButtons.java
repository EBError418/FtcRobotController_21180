package org.firstinspires.ftc.teamcode;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.Gamepad;


/**
 * Used to detect cone position, and sleeve color.
 */
public class GamePadButtons {
    //game pad setting
    public float robotDrive;
    public float robotStrafe;
    public float robotTurn;
    public float sliderUpDown;
    public boolean sliderWallPosition;
    public boolean sliderGroundJunction;
    public boolean sliderLowJunction;
    public boolean sliderMediumJunction;
    public boolean sliderHighJunction;
    public boolean sliderSkipLimitation;
    public boolean sliderResetEncoder;
    public boolean clawClose;
    public boolean clawOpen;
    public boolean armLeft;
    public boolean armRight;
    public boolean armForward;
    public boolean autoLoadGroundCone;
    public boolean autoLoad34thConeStack;
    public boolean autoLoad5thConeStack;
    public boolean autoUnloadCone;

    public void checkGamepadButtons(@NonNull Gamepad gamepad1, @NonNull Gamepad gamepad2) {
        //gamepad1 buttons
        robotDrive              = gamepad1.left_stick_y;
        robotStrafe             = gamepad1.left_stick_x;
        robotTurn               = gamepad1.right_stick_x;
        autoLoadGroundCone      = gamepad1.left_bumper;
        autoLoad34thConeStack   = gamepad1.dpad_up;
        autoLoad5thConeStack    = gamepad1.dpad_down;
        autoUnloadCone          = gamepad1.right_bumper;

        // gamepad1(single driver) or gamepad2(dual driver) buttons
        sliderUpDown            = gamepad2.right_stick_y;
        sliderGroundJunction    = gamepad2.dpad_left;
        sliderWallPosition      = gamepad2.x;
        sliderLowJunction       = gamepad2.a;
        sliderMediumJunction    = gamepad2.b;
        sliderHighJunction      = gamepad2.y;
        sliderSkipLimitation    = gamepad2.left_bumper;
        sliderResetEncoder      = (gamepad2.right_bumper && gamepad2.left_bumper);

        // gamepad1 or gamepad2
        clawClose               = gamepad2.dpad_up;
        clawOpen                = gamepad2.dpad_down || gamepad1.a;
        armLeft                 = (gamepad2.left_stick_x < 0);
        armRight                 = (gamepad2.left_stick_x > 0);
        armForward              = (gamepad2.left_stick_y < 0);
    }

}
