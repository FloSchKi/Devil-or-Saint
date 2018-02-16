package com.schulz_kittler.florian.devil_or_saint;

/**
 * Created by Schulz on 26.06.2017.
 */
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
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private Context mainContext;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    FaceGraphic(GraphicOverlay overlay, Context mainContext) {
        super(overlay);
        this.mainContext = mainContext;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
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
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = scaleX(face.getPosition().x);
        float y = scaleY(face.getPosition().y);

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

        float haloWidth = scaleX(face.getWidth());
        //float haloWidth = scaleX(leftEye.x) - scaleX(rightEye.x);

        //int left = Math.round(scaleX(rightEye.x));
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(left + haloWidth);
        int bottom = Math.round(y + (haloWidth/400*70));

        Drawable halo = mainContext.getDrawable(R.drawable.halo);
        halo.setBounds(left, top, right, bottom);
        halo.draw(canvas);

        /*canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        canvas.drawText("happiness: " + Math.round(face.getIsSmilingProbability() * 100) + "%", x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
        canvas.drawText("right eye: " + Math.round(face.getIsRightEyeOpenProbability() * 100) + "%", x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
        canvas.drawText("left eye: " + Math.round(face.getIsLeftEyeOpenProbability() * 100) + "%", x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);*/

        // Draws a bounding box around the face.
        /*float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);*/
        /*float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;*/
        //canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }
}