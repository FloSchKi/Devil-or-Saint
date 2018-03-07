package com.schulz_kittler.florian.devil_or_saint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.google.android.gms.vision.face.Landmark;
import com.schulz_kittler.florian.devil_or_saint.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private Paint mCreditsPaint;
    private volatile Face mFace;
    private Context mainContext;
    private int mFaceId;
    private int devilOrSaint = 0;
    private int credits = 0;
    private boolean cVisible = false;

    FaceGraphic(GraphicOverlay overlay, Context mainContext) {
        super(overlay);
        this.mainContext = mainContext;

        mCreditsPaint = new Paint();
        mCreditsPaint.setColor(Color.RED);
        mCreditsPaint.setTextSize(50.0f);
    }

    // Sets ID of the current face
    void setId(int id) {
        mFaceId = id;
    }

    /**
     *
     * @param doSCode can have three values:
     *                0 = no face filter
     *                1 = devil face filter
     *                2 = saint face filter
     */
    public void setDevilOrSaint(Integer doSCode) { devilOrSaint = doSCode; }

    /**
     * Sets the credits to draw on screen.
     * @param cred credits retrieved from the server
     */
    public void setCredits(Integer cred) {
        credits = cred;
    }

    /**
     * Sets if the Credits are visible or invisible.
     * @param visible visibility of the credits
     */
    public void setCreditsVisible(Boolean visible) {
        cVisible = visible;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face filters on the Overlay canvas if enough votes are returned from the server.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // transforms the x and y position of the face correctly
        float x = scaleX(face.getPosition().x);
        float y = scaleY(face.getPosition().y);

        if(devilOrSaint == 1) { // Devil Face Filter is shown
            // retrieves the position of the left and right eye
            PointF leftEye = new PointF(0,0);
            PointF rightEye = new PointF(0,0);
            for (Landmark landmark : face.getLandmarks()) {
                if(landmark.getType() == Landmark.LEFT_EYE) {
                    leftEye = landmark.getPosition();
                }
                if(landmark.getType() == Landmark.RIGHT_EYE) {
                    rightEye = landmark.getPosition();
                }
            }

            // calculates the width and height for the devil horns
            float devilWidth = scaleX(leftEye.x) - x;
            float leftEyePadding = (x + scaleX(face.getWidth())) - scaleX(leftEye.x);
            float devilHeight = devilWidth/400*70;

            // specifies the bounding box of the devil horns and where it should be drawn
            int left = Math.round(x + (scaleX(rightEye.x) - x)/2.0f);
            int top = Math.round(y + ((scaleY(leftEye.y) - y)/2.0f) - devilHeight);
            int right = Math.round(scaleX(leftEye.x) + leftEyePadding/2.0f);
            int bottom = Math.round(top + devilHeight*2.0f);

            // draws the devil horn image on the forehead of the provided face
            Drawable devil = mainContext.getDrawable(R.drawable.devil);
            devil.setBounds(left, top, right, bottom);
            devil.draw(canvas);
        } else if (devilOrSaint == 2) { // Saint Face Filter is shown
            float haloWidth = scaleX(face.getWidth());  // calculates the width of the halo drawn on top of the head

            // specifies the bounding box of the halo and where it should be drawn
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(left + haloWidth);
            int bottom = Math.round(y + (haloWidth/400*70));

            // draws the halo image over the head
            Drawable halo = mainContext.getDrawable(R.drawable.halo);
            halo.setBounds(left, top, right, bottom);
            halo.draw(canvas);
        }

        // checks the visibility of the credits and draws it
        if (cVisible) {
            canvas.drawText("Credits: " + String.valueOf(credits), canvas.getWidth()/2.0f - 100.0f, 50, mCreditsPaint);
        }
    }
}