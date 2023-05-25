package com.experimental.star_map.views;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.experimental.star_map.R;

public class StarMapView extends View {
    public StarMapView(Context context) {
        super(context);
    }

    public StarMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public StarMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {
        // запросить свойства, описанные в xml
        @SuppressLint("CustomViewStyleable") TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.StarMap, defStyle, 0);
        try {
            // считываем значение
            loadSkyMap(
                    styledAttributes.getResourceId(R.styleable.StarMap_baseImage, -1),
                    styledAttributes.getResourceId(R.styleable.StarMap_detailImage, -1)
            );
        } finally {
            // сообщаем Android о том, что данный объект можно переиспользовать
            styledAttributes.recycle();
        }

        transformationMatrix = new Matrix();
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        m = new float[9];
        center.x = base.getWidth() / 2f;
        center.y = base.getHeight() / 2f;
    }

    private void loadSkyMap(int baseId, int detailId) {
        base = (baseId == -1) ? null : BitmapFactory.decodeResource(getResources(), baseId);
        detail = (detailId == -1) ? null : BitmapFactory.decodeResource(getResources(), detailId);
    }

    private final Star[] starsData = new Star[]{
            new Star("Сириус", -16.65, 6 + 43.0 / 60),
            new Star("Конапус", -52.66, 6 + 23.0 / 60),
            new Star("Арктур", 19.45, 14 + 13.0 / 60),
            new Star("Вега", 38.73, 18 + 35.0 / 60),
            new Star("Капелла", 45.95, 5 + 13.0 / 60),
            new Star("Ригель", -8.25, 5 + 12.0 / 60),
            new Star("Процион", -5.35, 7 + 37.0 / 60),
            new Star("Ахернар", -57.5, 1 + 36.0 / 60),
            new Star("Альтаир", 8.73, 19 + 48.0 / 60),
            new Star("Бетельгейзе", 7.4, 5 + 53.0 / 60),
            new Star("Альдебаран", 16.42, 4 + 33.0 / 60),
            new Star("Спика", -10.9, 13 + 23.0 / 60),
            new Star("Антарес", -26.32, 13 + 24.0 / 60),
            new Star("Поллукс", 28.15, 7 + 42.0 / 60),
            new Star("Фомальгаут", -29.88, 22 + 55.0 / 60),
            new Star("Хадар", -60.13, 14 + 0.3 / 60),
            new Star("Поллукс", 28.15, 7 + 42.0 / 60),
            new Star("Бета Ю. Креста", -16.65, 20 + 40.0 / 60),
            new Star("Регул", 11.96, 10 + 8.0 / 60),
            new Star("Денеб", -59.42, 20 + 40.0 / 60)
    };
           // Крупные звезды, наблюдаемые на звездном небе
    State mode = State.NONE;
    private float[] m = null;

    static private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        paint.setTextSize(30f);
        paint.setStrokeWidth(1f);
    }

    // Начальное положение курсора
    private final PointF curr = new PointF(-10000F, -10000F);

    // Запомним некоторые моменты для увеличения масштаба
    private final PointF last = new PointF();
    private final PointF start = new PointF();

    private final PointF center = new PointF();
    private final float[] tempCenter = new float[2];
    private float angle = 0f;
    private boolean isShowed = false;

    public void switchMap() {
        isShowed = !isShowed;
    }

    private final Matrix tempMatrix = new Matrix();

    int viewWidth = 0;
    int viewHeight = 0;
    float saveScale = 1f;
    protected float origWidth = 0f;
    protected float origHeight = 0f;
    private int oldMeasuredWidth = 0;
    private int oldMeasuredHeight = 0;
    private ScaleGestureDetector mScaleDetector = null;
    private Bitmap base = null;
    private Bitmap detail = null;
    Matrix transformationMatrix = null;

    public void setAngle(float angle) {
        this.angle = angle;
    }

    private double getSectorTime(float x, float y) {
        int time = (int) (Math.toDegrees(Math.atan2((x - tempCenter[0]), (y - tempCenter[1]))) * 4 + angle % 360 * 4);
        time = (time < 0) ? -time : 1440 - time;

        return Math.round(time / 0.6) / 100.0;
    }

    // позиция на карте Прямого восхождения / Времени

    private double getSectorAngle(float x, float y) {
        int shortestSide = Math.min(viewWidth, viewHeight);

        double length = Math.sqrt(Math.pow((x - tempCenter[0]), 2) + Math.pow((y - tempCenter[1]), 2));
//        System.out.println("length: " + length + " " + shortestSide);

        if (length > shortestSide * saveScale) {
            return -1.0;
        } else if (length >= shortestSide * 0.38 * saveScale) {
            return Math.round((-15 * (length - shortestSide * 0.38 * saveScale) / (shortestSide * 0.07 * saveScale) - 30) * 100) / 100.0;
        } else if (length >= shortestSide * 0.26 * saveScale) {
            return Math.round((-30 * (length - shortestSide * 0.26 * saveScale) / (shortestSide * 0.12 * saveScale)) * 100) / 100.0;
        } else if (length >= shortestSide * 0.16 * saveScale) {
            return Math.round((-30 * (length - shortestSide * 0.16 * saveScale) / (shortestSide * 0.10 * saveScale) + 30) * 100) / 100.0;
        } else if (length >= shortestSide * 0.08 * saveScale) {
            return Math.round((-30 * (length - shortestSide * 0.08 * saveScale) / (shortestSide * 0.08 * saveScale) + 60) * 100) / 100.0;
        } else {
            return Math.round((-30 * length / (shortestSide * 0.08 * saveScale) + 90) * 100) / 100.0;
        }
    }

    // позиция на карте Склонения / Углового расстояния

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        int shortestSide = Math.min(viewWidth, viewHeight);

        // Закрасим фон темно-серым цветом
        canvas.drawColor(Color.DKGRAY);

        // Настройка матрицы преобразования
        tempCenter[0] = center.x;
        tempCenter[1] = center.y;
        transformationMatrix.mapPoints(tempCenter);
        tempMatrix.set(transformationMatrix);
        tempMatrix.postRotate(angle, tempCenter[0], tempCenter[1]);

        // Рисуем карту
        canvas.drawBitmap(base, tempMatrix, paint);

        // Рисуем вспомогательную деталь
        if (isShowed) {
            canvas.drawBitmap(detail, transformationMatrix, paint);
        }

        // Рисуем указатель
        paint.setColor(Color.RED);
        canvas.drawCircle(curr.x, curr.y, 5f, paint);

        double tempStarTime = getSectorTime(curr.x, curr.y);
        double tempStarAngle = getSectorAngle(curr.x, curr.y);

        boolean isFound = false;
        String star = "";
        for (Star i : starsData) {
            if (i.isDataStar(tempStarTime, tempStarAngle)) {
                isFound = true;
                star = i.getName();
                break;
            }
        }

        paint.setColor(Color.GRAY);

        if (isFound) {
            canvas.drawRoundRect(new RectF(curr.x + 5, curr.y + 5, curr.x + 260, curr.y + 120), 10f, 10f, paint);

            paint.setColor(Color.WHITE);
            canvas.drawText("t: " + tempStarTime, curr.x + 20, curr.y + 50, paint);
            canvas.drawText("a: " + tempStarAngle, curr.x + 130, curr.y + 50, paint);
            paint.setColor(Color.GREEN);
            canvas.drawText(star, curr.x + 20, curr.y + 90, paint);
        } else {
            canvas.drawRoundRect(new RectF(curr.x + 5, curr.y + 5, curr.x + 260, curr.y + 80), 10f, 10f, paint);

            paint.setColor(Color.WHITE);
            canvas.drawText("t: " + tempStarTime, curr.x + 20, curr.y + 50, paint);
            canvas.drawText("a: " + tempStarAngle, curr.x + 130, curr.y + 50, paint);
        }

        // Вывод указателя позиции на экран
        invalidate();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        curr.set(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                last.set(curr);
                start.set(last);
                mode = State.DRAG;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == State.DRAG) {
                    float deltaX = curr.x - last.x;
                    float deltaY = curr.y - last.y;
                    float fixTransX = getFixDragTrans(deltaX, viewWidth, origWidth * saveScale);
                    float fixTransY = getFixDragTrans(deltaY, viewHeight, origHeight * saveScale);
                    transformationMatrix.postTranslate(fixTransX, fixTransY);
                    fixTrans();
                    last.set(curr.x, curr.y);
                }
                break;
            case MotionEvent.ACTION_UP:
                mode = State.NONE;
                int xDiff = (int) Math.abs(curr.x - start.x);
                int yDiff = (int) Math.abs(curr.y - start.y);
                if (xDiff < 3 && yDiff < 3) performClick();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = State.NONE;
                break;
        }
        invalidate();
        return true;
    }


    // Масштабирование
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = State.ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            float minScale = 1f;
            float maxScale = 10f;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                transformationMatrix.postScale(
                        mScaleFactor,
                        mScaleFactor,
                        viewWidth / 2f,
                        viewHeight / 2f
                );
            } else {
                transformationMatrix.postScale(
                        mScaleFactor,
                        mScaleFactor,
                        detector.getFocusX(),
                        detector.getFocusY()
                );
            }

            fixTrans();

            return true;

        }
    }

    private void fixTrans() {
        transformationMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        float fixTransX = getFixTrans(transX, (float) viewWidth, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, (float) viewHeight, origHeight * saveScale);
        if (fixTransX != 0f || fixTransY != 0f) {
            transformationMatrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans;
        float maxTrans;
        if (contentSize <= viewSize) {
            minTrans = 0f;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0f;
        }
        if (trans < minTrans) return -trans + minTrans;
        return trans > maxTrans ? -trans + maxTrans : 0F;
    }

    private float getFixDragTrans(float delta, float viewSize, float contentSize) {
        return contentSize <= viewSize ? 0F : delta;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (oldMeasuredWidth == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0)
            return;

        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;

        if (saveScale == 1f) {

            // Подходит для экрана
            float scale;
            int bmWidth = base.getWidth();
            int bmHeight = base.getHeight();
            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            transformationMatrix.setScale(scale, scale);

            // Центрируем изображение
            float redundantYSpace = (float) viewHeight - scale * (float) bmHeight;
            float redundantXSpace = (float) viewWidth - scale * (float) bmWidth;
            redundantYSpace /= 2f;
            redundantXSpace /= 2f;
            transformationMatrix.postTranslate(redundantXSpace, redundantYSpace);
            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
        }

        fixTrans();
    }
}


enum State {NONE, DRAG, ZOOM}

class Star {
    private String name;
    private double angle;
    private double time;

    public Star(String name, double angle, double time) {
        this.name = name;
        this.angle = angle;
        this.time = time;
    }

    public boolean isDataStar(double time1, double angle1) {
        return (time <= time1 + 0.1 && time >= time1 - 0.1 && angle <= angle1 + 3 && angle >= angle1 - 3);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
