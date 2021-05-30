package com.android.wuliu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.android.dialer.R;
import com.android.dialer.common.LogUtil;

import java.util.ArrayList;

public class WuLiuTrackView extends View {

  private static final String TAG = "WuLiuTrackView";

  private ArrayList<WuLiuTrackInfoBean> trackList;

  private float columnWidth;
  private float nodeRadius;

  private Paint nodeLinePaint;
  private TextPaint addressPaint;

  private Context context;

  /**
   * 节点间隔
   */
  private int nodeDistance;

  private float nodeDistanceGap;

  /**
   * 边距
   */
  private float left = 40;
  private float itemTop;

  private float viewWidth;
  private float viewHeight;
  private TextPaint timePaint;


  public WuLiuTrackView(Context context) {
    super(context);
    this.context = context;
    init();
  }

  public WuLiuTrackView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    LogUtil.d(TAG, "View Measured:[" + getMeasuredWidth() + "," + getMeasuredHeight() + "]");
    LogUtil.d(TAG, "View:[" + getWidth() + "," + getHeight() + "]");
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WuLiuTrackView);
    columnWidth = typedArray.getDimension(R.styleable.WuLiuTrackView_columnWidth, 5);
    viewWidth = typedArray.getDimension(R.styleable.WuLiuTrackView_trackWidth,
      getResources().getDimension(R.dimen.incall_track_list_default_width));

    itemTop = typedArray.getDimension(R.styleable.WuLiuTrackView_trackItemTop, 5);
    left = typedArray.getDimension(R.styleable.WuLiuTrackView_trackItemLeft, 10);
    nodeRadius = typedArray.getDimension(R.styleable.WuLiuTrackView_nodeRadius, 10);
    init();
  }

  public WuLiuTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    nodeLinePaint = new Paint();
    nodeLinePaint.setColor(getResources().getColor(R.color.wu_liu_incall_track_time_line));
    nodeLinePaint.setAntiAlias(true);
    nodeLinePaint.setStrokeWidth(8);

    timePaint = new TextPaint();
    timePaint.setAntiAlias(true);
    timePaint.setColor(getResources().getColor(R.color.wu_liu_incall_track_time));
    timePaint.setTextSize(getResources().getDimension(R.dimen.incall_track_list_text_size));

    addressPaint = new TextPaint();
    addressPaint.setColor(getResources().getColor(R.color.wu_liu_incall_track_address));
    addressPaint.setTextSize(getResources().getDimension(R.dimen.incall_track_list_text_size));
    addressPaint.setAntiAlias(true);

    nodeDistance = (int) getResources().getDimension(R.dimen.incall_node_distance);
    nodeDistanceGap = getResources().getDimension(R.dimen.incall_node_distance_gap);
  }

  /**
   * 设置适配数据
   */
  public void setTrackList(ArrayList<WuLiuTrackInfoBean> list) {
    this.trackList = list;
    invalidate();
  }

  private float timeWidth = 120.0f;
  private float timeLineWidth = 5.0f;
  private float timeGapNode = 20.0f;

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (trackList == null || trackList.size() == 0)
      return;

    for (int i = 0; i < trackList.size(); i++) {

      StaticLayout layoutTime = new StaticLayout((trackList.get(i)).getScanTime() + "",
        timePaint, (int) (timeWidth), Layout.Alignment.ALIGN_NORMAL, 1.0F,
        0.0F, true);// 时间
      canvas.save();
      canvas.translate(left, i * nodeDistance);
      layoutTime.draw(canvas);
      canvas.restore();//重置

      //画圆点
      canvas.drawCircle(timeWidth + left + timeGapNode,
        i * nodeDistance + itemTop + nodeRadius * 2, nodeRadius, nodeLinePaint);
      // 画圆点之间连线
      if (i < trackList.size() - 1) {
        canvas.drawLine(timeWidth + left + timeGapNode,
          i * nodeDistance + nodeRadius * 2,
          timeWidth + left + timeGapNode,
          (i + 1) * nodeDistance + itemTop + nodeRadius * 2, nodeLinePaint); //画线
      }
      //文字换行
      StaticLayout layout = new StaticLayout((trackList.get(i)).getDesc() + "",
        addressPaint, (int) ((viewWidth - (timeWidth + left + timeGapNode * 2 + nodeRadius * 2) * 0.9)),
        Layout.Alignment.ALIGN_NORMAL,
        1.0F, 0.0F, true);
      canvas.save();//很重要，不然会样式出错
      canvas.translate(timeWidth + left + timeGapNode * 2 + nodeRadius * 2,
        i * nodeDistance + itemTop);
      layout.draw(canvas);
      canvas.restore();//重置
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (trackList == null || trackList.size() == 0)
      return;
    setMeasuredDimension(widthMeasureSpec, (int) (trackList.size() * nodeDistance + itemTop * 2));
  }
}
