package com.scoller.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.scoller.ChartAnimator;

import static com.scoller.chart.ScrollBar.Direction.RIGHT;


/**
 * Created by Fussen on 2017/4/21.
 * <p>
 * 可以滚动的条形图
 */
public class ScrollBar extends ViewGroup {
    private int mHeight;
    private int mWidth;
    private int mDuriation = 1300;
    private int mMinimumFlingVelocity = 0;
    private int pos = 1;
    private int mScrollDuration = 250;

    private float triangleLenght = 10;    //尖部三角形边长
    private float outSpace = 30f;// 柱子与纵轴的距离
    private float startChart = 50f; //柱子开始的横坐标
    private float interval = 90f;//柱子之间的间隔
    private float barWidth = 50f;//柱子的宽度
    private float bottomHeight = 20f;//底部横坐标高度
    private float mXScrollingSpeed = 1f;    //滑动速度


    private String maxValue = "2";//默认最大值
    private String middleValue = "1";
    //最大的柱子到顶端的距离
    private int paddingTop = 200;

    private Paint mChartPaint;
    private Paint textPaint;
    private Paint clickTextPaint;
    private Paint bubblePaint;
    private Paint linePaint;

    private Path bubblePath;

    private Context mContext;
    private ChartAnimator initmAnimator;
    private ChartAnimator clickmAnimator;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private List<RectF> rectList;
    private List<String> datas;
    private List<Float> verticalList = new ArrayList<>();

    public enum Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    private Direction mCurrentScrollDirection = Direction.NONE;    //正常滑动方向
    private Direction mCurrentFlingDirection = Direction.NONE;    //快速滑动方向
    private boolean mHorizontalFlingEnabled = true;
    private PointF mCurrentOrigin = new PointF(0f, 0f);


    private OnCliclListener listener;

    public void setOnClickListener(OnCliclListener listener) {
        this.listener = listener;
    }

    public ScrollBar(Context context) {
        this(context, null);
        this.mContext = context;
        init();
    }

    public ScrollBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.mContext = context;
        init();
    }

    public ScrollBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }

    private void init() {
        //初始化手势
        mGestureDetector = new GestureDetectorCompat(mContext, mGestureListener);
        // 解决长按屏幕后无法拖动的现象 但是 长按 用不了
        mGestureDetector.setIsLongpressEnabled(false);
        mScroller = new OverScroller(mContext, new FastOutLinearInInterpolator());
        mMinimumFlingVelocity = ViewConfiguration.get(mContext).getScaledMinimumFlingVelocity();
        //初始化动画
        initmAnimator = new ChartAnimator(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                postInvalidate();
            }
        });
        clickmAnimator = new ChartAnimator(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                postInvalidate();
            }
        });
        //柱子画笔
        mChartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChartPaint.setStyle(Paint.Style.FILL);
        //气泡提示框画笔
        bubblePaint = new Paint();
        bubblePaint.setTextAlign(Paint.Align.CENTER);
        bubblePaint.setTextSize(convertDpToPixel(10f));
        bubblePaint.setColor(Color.parseColor("#FFCC00"));
        //文字画笔
        textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(convertDpToPixel(10f));
        textPaint.setColor(Color.parseColor("#80ffffff"));
        //点击后文字画笔
        clickTextPaint = new Paint();
        clickTextPaint.setTextAlign(Paint.Align.CENTER);
        clickTextPaint.setTextSize(convertDpToPixel(10f));
        clickTextPaint.setColor(Color.parseColor("#ffffff"));
        //线画笔
        linePaint = new Paint();
        linePaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setColor(Color.parseColor("#26ffffff"));
        rectList = new ArrayList<>();
        datas = getMonthStrList();
        setOnClickListener(new OnCliclListener() {
            @Override
            public void onclicklistener(int position) {
                pos = position;
                Log.d("ggh", "点击了" + position);
                invalidate();
                clickmAnimator.animateY(500);
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //宽度的模式
        int mWidthModle = MeasureSpec.getMode(widthMeasureSpec);
        //宽度大小
        int mWidthSize = MeasureSpec.getSize(widthMeasureSpec);

        int mHeightModle = MeasureSpec.getMode(heightMeasureSpec);
        int mHeightSize = MeasureSpec.getSize(heightMeasureSpec);
        //如果明确大小,直接设置大小
        if (mWidthModle == MeasureSpec.EXACTLY) {
            mWidth = mWidthSize;
        } else {
            //计算宽度,可以根据实际情况进行计算
            mWidth = (getPaddingLeft() + getPaddingRight());
            //如果为AT_MOST, 不允许超过默认宽度的大小
            if (mWidthModle == MeasureSpec.AT_MOST) {
                mWidth = Math.min(mWidth, mWidthSize);
            }
        }
        if (mHeightModle == MeasureSpec.EXACTLY) {
            mHeight = mHeightSize;
        } else {
            mHeight = (getPaddingTop() + getPaddingBottom());
            if (mHeightModle == MeasureSpec.AT_MOST) {
                mHeight = Math.min(mHeight, mHeightSize);
            }
        }
        //设置测量完成的宽高
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mWidth = getWidth();
        mHeight = getHeight() - paddingTop;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float textHeight = mHeight + paddingTop - bottomHeight;//横坐标高度
        //控制图表滑动左右边界
        if (mCurrentOrigin.x < getWidth() - (verticalList.size() * barWidth + (verticalList.size() - 1) * interval + outSpace))
            mCurrentOrigin.x = getWidth() - (verticalList.size() * barWidth + (verticalList.size() - 1) * interval + outSpace + startChart);
        if (mCurrentOrigin.x > 0)
            mCurrentOrigin.x = 0;
        float chartTempStart = startChart;
        float size = (mHeight - bottomHeight) / 100f; //比例
        //画柱子
        drawBar(canvas, chartTempStart, size, textHeight);
    }

    /**
     * 画柱子
     *
     * @param canvas
     * @param chartTempStart
     * @param size
     */
    private void drawBar(Canvas canvas, float chartTempStart, float size, float textHeight) {
        rectList.clear();
        canvas.clipRect(outSpace - 10f, 0, mWidth, getHeight(), Region.Op.REPLACE);
        for (int i = 0; i < verticalList.size(); i++) {
            //每个数据点所占的Y轴高度
            float barHeight = verticalList.get(i) / Float.valueOf(maxValue) * 100f * size;
            float realBarHeight = barHeight * initmAnimator.getPhaseY();
            //设置柱形图透明度
            LinearGradient linearGradient = new LinearGradient(0, textHeight - barHeight, 0, textHeight, new int[]{Color.parseColor("#ffFFFFFF"), Color.parseColor("#00FFFFFF")}, new float[]{0, 1.0f}, Shader.TileMode.CLAMP);
            mChartPaint.setShader(linearGradient);
            //画柱状图 矩形
            RectF rectF = new RectF();

            rectF.top = (mHeight - bottomHeight + paddingTop) - realBarHeight;
            rectF.left = chartTempStart + mCurrentOrigin.x;
            rectF.right = chartTempStart + barWidth + mCurrentOrigin.x;

            rectF.bottom = mHeight + paddingTop - bottomHeight;
            rectList.add(rectF);
            //绘制柱子
            canvas.drawRoundRect(rectF, 10F, 10F, mChartPaint);

            if (i == pos) {
                RectF rectTip = new RectF(rectF.left - barWidth / 2, 100, rectF.right + barWidth / 2, 150);
                //绘制气泡
                bubblePath = drawBubble(rectTip);
                canvas.drawPath(bubblePath, bubblePaint);
                //保证动画正常
                float x1 = rectTip.bottom + triangleLenght;
                float x = rectF.top;
                //绘制气泡和柱子之间的线
                canvas.drawLine(rectTip.centerX(), rectTip.bottom + triangleLenght, rectTip.centerX(), x1 + (x - x1) * clickmAnimator.getPhaseY(), linePaint);
                //画顶部日期
                if (i == verticalList.size() - 1) {
                    canvas.drawText("今日", chartTempStart + mCurrentOrigin.x + 30, paddingTop / 3, clickTextPaint);
                } else {
                    canvas.drawText(datas.get(i), chartTempStart + mCurrentOrigin.x + 30, paddingTop / 3, clickTextPaint);
                }
                Paint.FontMetrics fontMetrics = clickTextPaint.getFontMetrics();
                float bottomLineY = rectTip.centerY() - (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.top;
                canvas.drawText(String.valueOf(1000 + 1000 * clickmAnimator.getPhaseY()), rectF.centerX(), bottomLineY, clickTextPaint);
            } else {
                //画顶部日期
                if (i == verticalList.size() - 1) {
                    canvas.drawText("今日", chartTempStart + mCurrentOrigin.x + 30, paddingTop / 3, textPaint);
                } else {
                    canvas.drawText(datas.get(i), chartTempStart + mCurrentOrigin.x + 30, paddingTop / 3, textPaint);
                }
            }
            chartTempStart += (barWidth + interval);
        }


    }

    /**
     * 设置纵轴数据
     *
     * @param verticalList
     */
    public void setVerticalList(List<Float> verticalList) {
        if (verticalList != null) {
            this.verticalList = verticalList;
            pos = datas.size() - 1;

        } else {
            maxValue = "2";
            middleValue = "1";
            invalidate();
            pos = 1;
            return;
        }

        if (Collections.max(verticalList) > 2) {
            int tempMax = Math.round(Collections.max(verticalList));
            while (tempMax % 10 != 0) {
                tempMax++;
            }
            int middle = tempMax / 2;
            maxValue = String.valueOf(tempMax);
            middleValue = String.valueOf(middle);
        } else {
            maxValue = "2";
            middleValue = "1";
        }

        initmAnimator.animateY(mDuriation);
        clickmAnimator.animateY(mDuriation);
    }


    /**
     * 绘制气泡提示框
     *
     * @param myRect
     * @return
     */
    private Path drawBubble(RectF myRect) {
        final Path path = new Path();
        final float left = myRect.left;
        final float top = myRect.top;
        final float right = myRect.right;
        final float bottom = myRect.bottom;
        final float centerX = myRect.centerX();

        //顶部线
        path.moveTo(left, top);
        path.lineTo(right, top);
        //右边
        path.lineTo(right, bottom);
        path.lineTo(centerX + triangleLenght, bottom);
        path.lineTo(centerX, myRect.bottom + triangleLenght);
        path.lineTo(centerX - triangleLenght, bottom);
        path.lineTo(left, bottom);
        path.lineTo(left, top);
        path.close();

        return path;
    }


    private static DisplayMetrics mMetrics;

    public float convertDpToPixel(float dp) {

        if (mMetrics == null) {
            Resources res = getResources();
            mMetrics = res.getDisplayMetrics();
            Log.e("MPChartLib-Utils",
                    "Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before calling Utils.convertDpToPixel(...). Otherwise conversion does not take place.");
            return dp;
        }

        DisplayMetrics metrics = mMetrics;
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }


    /**
     * 返回30天的字符串
     *
     * @return
     */
    public List<String> getMonthStrList() {
        Calendar theCa = Calendar.getInstance();
        theCa.setTime(new Date());
        theCa.add(Calendar.DATE, -29);//最后一个数字30可改，30天的意思
        Date start = theCa.getTime();
        return getDayList(start, new Date());
    }


    /**
     * 解析一个日期段之间的所有日期
     *
     * @param beginDateStr 开始日期
     * @param endDateStr   结束日期
     * @return
     */
    public static ArrayList getDayList(Date beginDateStr, Date endDateStr) {

        // 定义一些变量
        Date beginDate = null;
        Date endDate = null;

        Calendar beginGC = null;
        Calendar endGC = null;
        ArrayList list = new ArrayList();

        try {
            // 将字符串parse成日期
            beginDate = beginDateStr;
            endDate = endDateStr;

            // 设置日历
            beginGC = Calendar.getInstance();
            beginGC.setTime(beginDate);

            endGC = Calendar.getInstance();
            endGC.setTime(endDate);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");

            // 直到两个时间相同
            while (beginGC.getTime().compareTo(endGC.getTime()) <= 0) {

                list.add(sdf.format(beginGC.getTime()));
                // 以日为单位，增加时间
                beginGC.add(Calendar.DAY_OF_MONTH, 1);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 滑动手势
     */
    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        //手指按下
        @Override
        public boolean onDown(MotionEvent e) {
            goToNearestBar();
            float x = e.getX();
            float y = e.getY();
            Iterator<RectF> it = rectList.iterator();
            while (it.hasNext()) {
                RectF rectF = it.next();
                if (rectF.contains(x, y)) {
                    if (listener != null) {
                        listener.onclicklistener(rectList.indexOf(rectF));
                    }
                    break;
                }
            }
            return true;
        }

        //有效的滑动
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            switch (mCurrentScrollDirection) {
                case NONE:
                    Log.d("ggh", "none");
                    // 只允许在一个方向上滑动
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            mCurrentScrollDirection = Direction.LEFT;
                        } else {
                            mCurrentScrollDirection = RIGHT;
                        }
                    } else {
                        mCurrentScrollDirection = Direction.VERTICAL;
                    }
                    break;
                case LEFT:
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && (distanceX < 0)) {
                        Log.d("ggh", "LEFT");
                        mCurrentScrollDirection = RIGHT;
                    }
                    break;
                case RIGHT:
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && (distanceX > 0)) {
                        Log.d("ggh", "RIGHT");
                        mCurrentScrollDirection = Direction.LEFT;
                    }
                    break;
            }
            // 重新计算滑动后的起点
            switch (mCurrentScrollDirection) {
                case LEFT:
                case RIGHT:
                    mCurrentOrigin.x -= distanceX * mXScrollingSpeed;
                    ViewCompat.postInvalidateOnAnimation(ScrollBar.this);
                    break;
            }
            return true;
        }

        //快速滑动
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if ((mCurrentFlingDirection == Direction.LEFT && !mHorizontalFlingEnabled) ||
                    (mCurrentFlingDirection == RIGHT && !mHorizontalFlingEnabled)) {
                return true;
            }
            mCurrentFlingDirection = mCurrentScrollDirection;
            mScroller.forceFinished(true);

            switch (mCurrentFlingDirection) {
                case LEFT:
                case RIGHT:
                    mScroller.fling((int) mCurrentOrigin.x, (int) mCurrentOrigin.y, (int) (velocityX * mXScrollingSpeed), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
                    break;
                case VERTICAL:
                    break;
            }
            ViewCompat.postInvalidateOnAnimation(ScrollBar.this);
            return true;
        }

        //单击事件
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        //长按
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }
    };


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.isFinished()) {//当前滚动是否结束
            goToNearestBar();
        } else {

            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) { //惯性滑动时保证最左边条目展示正确
                goToNearestBar();
            } else if (mScroller.computeScrollOffset()) {//滑动是否结束 记录最新的滑动的点 惯性滑动处理
                mCurrentOrigin.y = mScroller.getCurrY();
                mCurrentOrigin.x = mScroller.getCurrX();
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

    }


    /**
     * Check if scrolling should be stopped.
     *
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private boolean forceFinishScroll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return mScroller.getCurrVelocity() <= mMinimumFlingVelocity;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //将view的OnTouchEvent事件交给手势监听器处理
        boolean val = mGestureDetector.onTouchEvent(event);
        // 正常滑动结束后 处理最左边的条目
        if (event.getAction() == MotionEvent.ACTION_UP && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestBar();
            }
            mCurrentScrollDirection = Direction.NONE;
        }
        return val;
    }

    private void goToNearestBar() {

        //让最左边的条目 显示出来
        double leftBar = mCurrentOrigin.x / (barWidth + interval);
        if (mCurrentFlingDirection != Direction.NONE) {
            // 跳到最近一个bar
            leftBar = Math.round(leftBar);
        } else if (mCurrentScrollDirection == Direction.LEFT) {
            // 跳到上一个bar
            leftBar = Math.floor(leftBar);
        } else if (mCurrentScrollDirection == RIGHT) {
            // 跳到下一个bar
            leftBar = Math.ceil(leftBar);
        } else {
            // 跳到最近一个bar
            leftBar = Math.round(leftBar);
        }
        int nearestOrigin = (int) (mCurrentOrigin.x - leftBar * (barWidth + interval));
        if (nearestOrigin != 0) {
            // 停止当前动画
            mScroller.forceFinished(true);
            //开始滚动
            mScroller.startScroll((int) mCurrentOrigin.x, (int) mCurrentOrigin.y, -nearestOrigin, 0, (int) (Math.abs(nearestOrigin) / (barWidth + interval) * mScrollDuration));
            ViewCompat.postInvalidateOnAnimation(ScrollBar.this);
        }
        //重新设置滚动方向.
        mCurrentScrollDirection = mCurrentFlingDirection = Direction.NONE;

    }

    public void scollTo() {
        mScroller.startScroll(0, 0, Integer.MIN_VALUE, 0);
    }

    public interface OnCliclListener {
        void onclicklistener(int position);
    }

}