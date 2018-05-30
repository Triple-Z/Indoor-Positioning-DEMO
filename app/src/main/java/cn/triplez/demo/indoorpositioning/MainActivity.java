package cn.triplez.demo.indoorpositioning;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.kiba.coordinateaxischart.ChartConfig;
import com.kiba.coordinateaxischart.CoordinateAxisChart;
import com.kiba.coordinateaxischart.SinglePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;

public class MainActivity extends AppCompatActivity {
    // 允许的蓝牙设备MAC及编号
    private HashMap<String, Integer> allowBluetoothDeviceMacs = new HashMap<String, Integer>(){{
        // 初始化允许的蓝牙设备
        put("1F:3F:B4:37:92:42", 0);
//        put("EC:F4:A0:9B:56:23", 0);  // For test
        put("06:88:71:CD:AE:A7", 1);
        put("06:41:18:56:D1:64", 2);
    }};
    // 蓝牙设备对象
    private ArrayList<BleDevice> bluetoothDevices = new ArrayList<BleDevice>() {{
        add(null);
        add(null);
        add(null);
    }};
    // 设备就绪状态
    private boolean [] bluetoothReadyStates = new boolean[] {false, false, false};

    // 尝试增加的距离步长
    private static final double TRY_DISTANCE_STEP = 0.01;

//    private TextView bleDevice1Ready;
//    private TextView bleDevice2Ready;
//    private TextView bleDevice3Ready;

    private CardView bleCard1;
    private CardView bleCard2;
    private CardView bleCard3;

    private TextView bleDevice1Rssi;
    private TextView bleDevice2Rssi;
    private TextView bleDevice3Rssi;
    private EditText roomX;
    private EditText roomY;
    private Button refreshButton;
//    private Button calculateButton;
    private TextView location;
    private CoordinateAxisChart coordinateAxisChart;
    private FloatingActionButton fab;
    private CoordinatorLayout myCoordinatorLayout;
    private Snackbar snackbar;



    private void calculate() {
        // 先判断三个设备是否都已经准备就绪
        for (int i = 0; i < 2; i++) {
            if (!bluetoothReadyStates[i]) {
                int number = i + 1;
//                Toast.makeText(getApplicationContext(), "Cannot get No." + number + " beacon's RSSI.\n Get location FAILED!",
//                        Toast.LENGTH_LONG).show();
                if(snackbar != null) snackbar.dismiss();
                snackbar.make(myCoordinatorLayout, "Cannot get No." + number + " beacon's RSSI.\nGet location FAILED!", Snackbar.LENGTH_INDEFINITE).show();
                return;
            }
        }

        // 如果全部准备就绪了，则开始计算
        // 定义三个设备的坐标

        MathTool.Point device1Point, device2Point, device3Point;

        try {
            device1Point = new MathTool.Point(0, 0);
            device2Point = new MathTool.Point(Double.parseDouble(roomX.getText().toString()), 0);
            device3Point = new MathTool.Point(0, Double.parseDouble(roomY.getText().toString()));
        } catch (Exception e) {
            if(snackbar != null) snackbar.dismiss();
            snackbar.make(myCoordinatorLayout, "Invalid x or y range !", Snackbar.LENGTH_INDEFINITE).show();
            return;
        }

        // 将三个rssi都转换成实际的距离
        double [] distances = new double[3];
        for (int i = 0; i < 3; i++) {
            switch (i) {
//                case 0:
//                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi(), 3.3) * Math.cos(Math.toRadians(45));
//                    break;
//                case 1:
//                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi(), 3.3) * Math.cos(Math.toRadians(45));
//                    break;
//                case 2:
//                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi(), 2.7) * Math.cos(Math.toRadians(45));
//                    break;
                default:
                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi()) * Math.cos(Math.toRadians(45));
                    break;
            }
            Log.d("RSSI to distance : ", Double.toString(distances[i]));
        }

        // 抽象圆
        MathTool.Circle circle1 = new MathTool.Circle(
                new MathTool.Point(device1Point.x, device1Point.y),
                distances[0]);
        MathTool.Circle circle2 = new MathTool.Circle(
                new MathTool.Point(device2Point.x, device2Point.y),
                distances[1]
        );
        MathTool.Circle circle3 = new MathTool.Circle(
                new MathTool.Point(device3Point.x, device3Point.y),
                distances[2]
        );
        // 尝试进行运算
        while (true) {
            // 先看三个圆之间是否各自都有交点
            // 如果1、2两个圆之间没有交点
            if (!MathTool.isTwoCircleIntersect(circle1, circle2)) {
//                if(snackbar != null) snackbar.dismiss();
//                snackbar.make(myCoordinatorLayout, "Get location FAILED !", Snackbar.LENGTH_INDEFINITE).show();
//                return;
                // 尝试增加某个圆的半径，谁半径更大增加谁的
                if (circle1.r > circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP;
                } else {
                    circle2.r += TRY_DISTANCE_STEP;
                }
                continue;
            }
            // 如果1、3两个圆之间没有交点
            if (!MathTool.isTwoCircleIntersect(circle1, circle3)) {
//                if(snackbar != null) snackbar.dismiss();
//                snackbar.make(myCoordinatorLayout, "Get location FAILED !", Snackbar.LENGTH_INDEFINITE).show();
//                return;
                // 尝试增加半径
                // 如果c3的半径比两者之中任意一个都小
                if (circle3.r < circle1.r && circle3.r < circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP;
                    circle2.r += TRY_DISTANCE_STEP;
                } else {
                    circle3.r += TRY_DISTANCE_STEP;
                }
                continue;
            }
            // 如果2、3两个原之间没有交点
            if (!MathTool.isTwoCircleIntersect(circle2, circle3)) {
//                if(snackbar != null) snackbar.dismiss();
//                snackbar.make(myCoordinatorLayout, "Get location FAILED !", Snackbar.LENGTH_INDEFINITE).show();
//                return;
                // 尝试增加半径
                // 如果c3的半径比两者之中任意一个都小
                if (circle3.r < circle1.r && circle3.r < circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP;
                    circle2.r += TRY_DISTANCE_STEP;
                } else {
                    circle3.r += TRY_DISTANCE_STEP;
                }
                continue;
            }

            // 等尝试到三个圆都有交点的时候，求出各自两个圆之间的交点
            MathTool.PointVector2 temp1 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle1, circle2);
            MathTool.PointVector2 temp2 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle2, circle3);
            MathTool.PointVector2 temp3 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle3, circle1);
            // 1、2两圆的交点取y > 0 的那个点
            MathTool.Point resultPoint1 = temp1.p1.y > 0 ?
                    new MathTool.Point(temp1.p1.x, temp1.p1.y):
                    new MathTool.Point(temp1.p2.x, temp1.p2.y);
            Log.d("resultPoint1", temp1.p1.toString() + "  " + temp1.p2.toString());
            // 2、3两圆的交点取两者的均值
//            MathTool.Point resultPoint2 = new MathTool.Point(
//                    (temp2.p1.x + temp2.p2.x) / 2,
//                    (temp2.p1.y + temp2.p2.y) / 2
//            );
            MathTool.Point resultPoint2 = new MathTool.Point(
                    max(temp2.p1.x, temp2.p2.x),
                    max(temp2.p1.y, temp2.p2.y)
            );
            // 3、1两圆的交点取x > 0的那个点
            MathTool.Point resultPoint3 = temp3.p1.x > 0 ?
                    new MathTool.Point(temp3.p1.x, temp3.p1.y):
                    new MathTool.Point(temp3.p2.x, temp3.p2.y);

            // 求出三个点的中心点
            MathTool.Point resultPoint = MathTool.getCenterOfThreePoint(
                    resultPoint1,
                    resultPoint2,
                    resultPoint3
            );

            Log.d("Location", resultPoint1.toString() + "  " + resultPoint2.toString() + "  " + resultPoint3.toString());

            // 更新结果显示
//            Toast.makeText(getApplicationContext(), "Get the location!", Toast.LENGTH_SHORT).show();
            if(snackbar != null) snackbar.dismiss();
            snackbar.make(myCoordinatorLayout, "Get the location!", Snackbar.LENGTH_LONG).show();

            location.setText(resultPoint.toString());

            float x_float = (float)resultPoint.x;
            float y_float = (float)resultPoint.y;

            ChartConfig config = new ChartConfig();


            // the max value of the axis 坐标轴的最大值
//            config.setMax(10);
            try {
                config.setMax(max(Integer.parseInt(roomX.getText().toString()), Integer.parseInt(roomY.getText().toString())));
            } catch (Exception e) {
//                Toast.makeText(getApplicationContext(), "Invalid x or y range !", Toast.LENGTH_SHORT).show();
                if(snackbar != null) snackbar.dismiss();
                snackbar.make(myCoordinatorLayout, "Invalid x or y range !", Snackbar.LENGTH_INDEFINITE).show();
                return;
            }

            /*
                The precision of tangent lines of the points on the function line
                recommended value: 1-10
                函数图像上的点的切线的精度 推荐值：1-10
            */
            config.setPrecision(1);

            /*
                The x axis will be equally separated to some segment points according to segmentSize
                and will connect these points when drawing the function.
                将x轴分割成segmentSize个点，成像时会将这些点连接起来。
            */
            config.setSegmentSize(50);

            coordinateAxisChart.setConfig(config);

            coordinateAxisChart.reset();
            coordinateAxisChart.invalidate();

            SinglePoint locPoint = new SinglePoint(new PointF(x_float, y_float));
            locPoint.setPointColor(Color.RED);
            coordinateAxisChart.addPoint(locPoint);

            float x_range_float, y_range_float;

            try {
                x_range_float = (float)Integer.parseInt(roomX.getText().toString());
                y_range_float = (float)Integer.parseInt(roomY.getText().toString());
            } catch (Exception e) {
                if(snackbar != null) snackbar.dismiss();
                snackbar.make(myCoordinatorLayout, "Invalid x or y range !", Snackbar.LENGTH_LONG).show();
                return;
            }

            SinglePoint ble1Point = new SinglePoint(new PointF(0, 0));
            SinglePoint ble2Point = new SinglePoint(new PointF(x_range_float, 0));
            SinglePoint ble3Point = new SinglePoint(new PointF(0, y_range_float));
            ble1Point.setPointColor(Color.GREEN);
            ble2Point.setPointColor(Color.GREEN);
            ble3Point.setPointColor(Color.GREEN);
            coordinateAxisChart.addPoint(ble1Point);
            coordinateAxisChart.addPoint(ble2Point);
            coordinateAxisChart.addPoint(ble3Point);

            coordinateAxisChart.invalidate();

            // 跳出循环
            break;
        }
    }


    private void scan() {
        bluetoothDevices.clear();
        for (int i = 0; i < 3; i++) bluetoothDevices.add(null);
        // 配置扫描规则
        BleManager.getInstance()
                .initScanRule(new BleScanRuleConfig.Builder()
                        .setAutoConnect(false)
                        .setScanTimeOut(5000)
                        .build()
                );
        // 打开蓝牙
        BleManager.getInstance().enableBluetooth();
        // 开始扫描
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
//                Toast.makeText(getApplicationContext(), "Finish Scanning!", Toast.LENGTH_SHORT)
//                        .show();
                fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                fab.setClickable(true);
                calculate();
            }

            @Override
            public void onScanStarted(boolean success) {
//                Toast.makeText(getApplicationContext(), "Locating...", Toast.LENGTH_SHORT).show();
                if(snackbar != null) snackbar.dismiss();
                snackbar.make(myCoordinatorLayout, "Locating...", Snackbar.LENGTH_INDEFINITE).show();
                fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.disabled)));
                fab.setClickable(false);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                // 如果扫描到了一个新设备
                // 看他是不是预先设定的那几个设备
                for (String mac : allowBluetoothDeviceMacs.keySet()) {
                    Log.d("BL", "get an bl device");
                    // 如果是
                    if (mac.equals(bleDevice.getMac())) {
                        // 获取index
                        int index = allowBluetoothDeviceMacs.get(mac);
                        // 将设备加入list中
                        bluetoothDevices.remove(index);
                        bluetoothDevices.add(index, bleDevice);

                        // 更新显示状态
                        bluetoothReadyStates[index] = true;
                        switch (index) {
                            case 0:
//                                bleDevice1Ready.setText("就绪");
                                bleCard1.setCardBackgroundColor(getResources().getColor(R.color.success));
                                bleDevice1Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            case 1:
//                                bleDevice2Ready.setText("就绪");
                                bleCard2.setCardBackgroundColor(getResources().getColor(R.color.success));
                                bleDevice2Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            case 2:
//                                bleDevice3Ready.setText("就绪");
                                bleCard3.setCardBackgroundColor(getResources().getColor(R.color.success));
                                bleDevice3Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        });
    }

    private void bindComponent() {
        // 绑定所有组件
//        bleDevice1Ready = findViewById(R.id.bleDevice1_ready);
//        bleDevice2Ready = findViewById(R.id.bleDevice2_ready);
//        bleDevice3Ready = findViewById(R.id.bleDevice3_ready);

        bleCard1 = (CardView)findViewById(R.id.bleCard1);
        bleCard2 = (CardView)findViewById(R.id.bleCard2);
        bleCard3 = (CardView)findViewById(R.id.bleCard3);

        bleDevice1Rssi = findViewById(R.id.bleDevice1_rssi);
        bleDevice2Rssi = findViewById(R.id.bleDevice2_rssi);
        bleDevice3Rssi = findViewById(R.id.bleDevice3_rssi);
        roomX = findViewById(R.id.room_x);
        roomY = findViewById(R.id.room_y);
//        refreshButton = findViewById(R.id.refresh_button);
//        calculateButton = findViewById(R.id.calculate_button);
        location = findViewById(R.id.location);

        coordinateAxisChart = (CoordinateAxisChart)findViewById(R.id.coordinateAxisChart);

        fab = (FloatingActionButton)findViewById(R.id.fab);
        myCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.myCoordinatorLayout);



        // 设置回调
        // 刷新状态按钮回调
//        refreshButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 更改显示状态为默认状态
//                for (int i = 0; i < 2; i++) {
//                    bluetoothReadyStates[i] = false;
//                }
////                bleDevice1Ready.setText("未就绪");
//                bleCard1.setCardBackgroundColor(getResources().getColor(R.color.error));
//                bleDevice1Rssi.setText("N/A");
////                bleDevice2Ready.setText("未就绪");
//                bleCard2.setCardBackgroundColor(getResources().getColor(R.color.error));
//                bleDevice2Rssi.setText("N/A");
////                bleDevice3Ready.setText("未就绪");
//                bleCard3.setCardBackgroundColor(getResources().getColor(R.color.error));
//                bleDevice3Rssi.setText("N/A");
//                // 开始一轮新的扫描
//                scan();
//            }
//        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 更改显示状态为默认状态
                for (int i = 0; i < 2; i++) {
                    bluetoothReadyStates[i] = false;
                }
//                bleDevice1Ready.setText("未就绪");
                bleCard1.setCardBackgroundColor(getResources().getColor(R.color.error));
                bleDevice1Rssi.setText("N/A");
//                bleDevice2Ready.setText("未就绪");
                bleCard2.setCardBackgroundColor(getResources().getColor(R.color.error));
                bleDevice2Rssi.setText("N/A");
//                bleDevice3Ready.setText("未就绪");
                bleCard3.setCardBackgroundColor(getResources().getColor(R.color.error));
                bleDevice3Rssi.setText("N/A");
                // 开始一轮新的扫描
                scan();
            }
        });


        // 计算按钮回调
//        calculateButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 先判断三个设备是否都已经准备就绪
//                for (int i = 0; i < 2; i++) {
//                    if (!bluetoothReadyStates[i]) {
//                        Toast.makeText(getApplicationContext(), "设备" + i + "未就绪，无法启动计算",
//                                Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                }
//
//                // 如果全部准备就绪了，则开始计算
//                // 定义三个设备的坐标
//                MathTool.Point device1Point = new MathTool.Point(0, 0);
//                MathTool.Point device2Point = new MathTool.Point(Double.parseDouble(roomX.getText().toString()), 0);
//                MathTool.Point device3Point = new MathTool.Point(0, Double.parseDouble(roomY.getText().toString()));
//
//                // 将三个rssi都转换成实际的距离
//                double [] distances = new double[3];
//                for (int i = 0; i < 3; i++) {
//                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi()) * Math.cos(Math.toRadians(45));
//                    Log.d("RSSI to distance : ", Double.toString(distances[i]));
//                }
//
//                // 抽象圆
//                MathTool.Circle circle1 = new MathTool.Circle(
//                        new MathTool.Point(device1Point.x, device1Point.y),
//                        distances[0]);
//                MathTool.Circle circle2 = new MathTool.Circle(
//                        new MathTool.Point(device2Point.x, device2Point.y),
//                        distances[1]
//                );
//                MathTool.Circle circle3 = new MathTool.Circle(
//                        new MathTool.Point(device3Point.x, device3Point.y),
//                        distances[2]
//                );
//                // 尝试进行运算
//                while (true) {
//                    // 先看三个圆之间是否各自都有交点
//                    // 如果1、2两个圆之间没有交点
//                    if (!MathTool.isTwoCircleIntersect(circle1, circle2)) {
//                        // 尝试增加某个圆的半径，谁半径更大增加谁的
//                        if (circle1.r > circle2.r) {
//                            circle1.r += TRY_DISTANCE_STEP;
//                        } else {
//                            circle2.r += TRY_DISTANCE_STEP;
//                        }
//                        continue;
//                    }
//                    // 如果1、3两个圆之间没有交点
//                    if (!MathTool.isTwoCircleIntersect(circle1, circle3)) {
//                        // 尝试增加半径
//                        // 如果c3的半径比两者之中任意一个都小
//                        if (circle3.r < circle1.r && circle3.r < circle2.r) {
//                            circle1.r += TRY_DISTANCE_STEP;
//                            circle2.r += TRY_DISTANCE_STEP;
//                        } else {
//                            circle3.r += TRY_DISTANCE_STEP;
//                        }
//                        continue;
//                    }
//                    // 如果2、3两个原之间没有交点
//                    if (!MathTool.isTwoCircleIntersect(circle2, circle3)) {
//                        // 尝试增加半径
//                        // 如果c3的半径比两者之中任意一个都小
//                        if (circle3.r < circle1.r && circle3.r < circle2.r) {
//                            circle1.r += TRY_DISTANCE_STEP;
//                            circle2.r += TRY_DISTANCE_STEP;
//                        } else {
//                            circle3.r += TRY_DISTANCE_STEP;
//                        }
//                        continue;
//                    }
//
//                    // 等尝试到三个圆都有交点的时候，求出各自两个圆之间的交点
//                    MathTool.PointVector2 temp1 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle1, circle2);
//                    MathTool.PointVector2 temp2 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle2, circle3);
//                    MathTool.PointVector2 temp3 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle3, circle1);
//                    // 1、2两圆的交点取y > 0 的那个点
//                    MathTool.Point resultPoint1 = temp1.p1.y > 0 ?
//                            new MathTool.Point(temp1.p1.x, temp1.p1.y) :
//                            new MathTool.Point(temp1.p2.x, temp1.p2.y);
//                    // 2、3两圆的交点取两者的均值
//                    MathTool.Point resultPoint2 = new MathTool.Point(
//                            (temp2.p1.x + temp2.p2.x) / 2,
//                            (temp2.p1.y + temp2.p2.y) / 2
//                    );
//                    // 3、1两圆的交点取x > 0的那个点
//                    MathTool.Point resultPoint3 = temp3.p1.x > 0 ?
//                            new MathTool.Point(temp3.p1.x, temp3.p1.y) :
//                            new MathTool.Point(temp3.p2.x, temp3.p2.y);
//
//                    // 求出三个点的中心点
//                    MathTool.Point resultPoint = MathTool.getCenterOfThreePoint(
//                            resultPoint1,
//                            resultPoint2,
//                            resultPoint3
//                    );
//
//                    // 更新结果显示
//                    location.setText(resultPoint.toString());
//
//                    float x_float = (float)resultPoint.x;
//                    float y_float = (float)resultPoint.y;
//
//                    coordinateAxisChart.reset();
//                    coordinateAxisChart.invalidate();
//
//                    SinglePoint point = new SinglePoint(new PointF(x_float, y_float));
//                    point.setPointColor(Color.RED);
//                    coordinateAxisChart.addPoint(point);
//                    coordinateAxisChart.invalidate();
//
//                    // 跳出循环
//                    break;
//                }
//            }
//        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setContentView(R.layout.material_main);
        // Ble初始化
        BleManager.getInstance().init(getApplication());

        // 绑定组件
        bindComponent();
    }
}
