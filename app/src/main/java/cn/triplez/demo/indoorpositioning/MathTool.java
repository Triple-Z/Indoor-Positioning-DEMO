package cn.triplez.demo.indoorpositioning;

public class MathTool {
    private final static double RSSI_TO_DISTANCE_A = 65;
    private final static double RSSI_TO_DISTANCE_N = 4;

    public static class Point {
        public double x;
        public double y;
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    public static class PointVector2 {
        public Point p1;
        public Point p2;
        public PointVector2(Point p1, Point p2) {
            this.p1 = new Point(p1.x, p1.y);
            this.p2 = new Point(p2.x, p2.y);
        }
    }

    public static class Circle {
        public Point center;
        public double r;
        public Circle(Point center, double r) {
            this.center = new Point(center.x, center.y);
            this.r = r;
        }
    }

    // 信号强度转距离
    public static double rssiToDistance(double rssi) {
        return Math.pow(10, (Math.abs(rssi) - RSSI_TO_DISTANCE_A) / (10 * RSSI_TO_DISTANCE_N));
    }

    // 获取两个点之间的距离
    public static double getDistanceBetweenTwoPoint(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    // 判断两个圆是否相交
    public static boolean isTwoCircleIntersect(Circle c1, Circle c2) {
        return getDistanceBetweenTwoPoint(c1.center, c2.center) < c1.r + c2.r;
    }

    // 求两个相交圆的交点
    public static PointVector2 getIntersectionPointsOfTwoIntersectCircle(Circle c1, Circle c2) {
        // 用于返回结果的容器
        PointVector2 pointVector2 = new PointVector2(new Point(0, 0), new Point(0, 0));
        // 如果 c1、c2 都在x轴上
        if (c1.center.y == c2.center.y && c1.center.y == 0) {
            // 看哪一个圆的圆心比较靠近原点
            Circle ct1 = c1.center.x < c2.center.x ?
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r) :
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r);
            Circle ct2 = c1.center.x < c2.center.x ?
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r) :
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r);
            // 圆心距
            double l = getDistanceBetweenTwoPoint(ct1.center, ct2.center);
            // 计算交点与x轴构成的角的余弦
            double cos = (ct1.r * ct1.r + l * l - ct2.r * ct2.r) / (2 * ct1.r * l);
            // 计算正弦
            double sin = Math.sqrt(1 - cos * cos);
            // 得出坐标
            pointVector2.p1.x = ct1.center.x + ct1.r * cos;
            pointVector2.p2.x = pointVector2.p1.x;
            pointVector2.p1.y = ct1.r * sin;
            pointVector2.p2.y = 0 - pointVector2.p1.y;
            return pointVector2;
        }
        // 如果 c1、c2 都在y轴上
        if (c1.center.x ==  c2.center.x && c1.center.x == 0) {
            // 看哪一个圆比较靠近原点
            Circle ct1 = c1.center.y < c2.center.y ?
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r) :
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r);
            Circle ct2 = c1.center.y < c2.center.y ?
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r) :
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r);
            // 圆心距
            double l = getDistanceBetweenTwoPoint(ct1.center, ct2.center);
            // 计算交点与y轴构成的叫的余弦
            double cos = (ct1.r * ct1.r + l * l - ct2.r * ct2.r) / (2 * ct1.r * l);
            // 计算正弦
            double sin = Math.sqrt(1 - cos * cos);
            // 得出坐标
            pointVector2.p1.y = ct1.center.y + ct1.r * cos;
            pointVector2.p2.y = pointVector2.p1.y;
            pointVector2.p1.x = ct1.r * sin;
            pointVector2.p2.x = 0 - pointVector2.p1.x;
            return pointVector2;
        }
        // 如果一个圆的圆心在x轴上，一个圆的圆心在y轴上
        if (c1.center.x == 0 && c2.center.y == 0 || c2.center.x == 0 && c1.center.y == 0) {
            // 将圆心在y轴上的圆设置为ct1，将圆心在x轴上的圆设置为ct2
            Circle ct1 = c1.center.x == 0 ?
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r) :
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r);
            Circle ct2 = c1.center.y == 0 ?
                    new Circle(new Point(c1.center.x, c1.center.y), c1.r) :
                    new Circle(new Point(c2.center.x, c2.center.y), c2.r);
            // 圆心距
            double l = getDistanceBetweenTwoPoint(ct1.center, ct2.center);
            // 求出a角的cos
            double aCos = (ct1.r * ct1.r + l * l - ct2.r * ct2.r) / (2 * ct1.r * l);
            // 求出a角的弧度
            double aAngle = Math.acos(aCos);
            // 求出b角的tan
            double aTan = ct2.center.x / ct1.center.y;
            // 求出b角的弧度
            double bAngle = Math.atan(aTan);
            // 得出坐标
            pointVector2.p1.x = ct1.center.x + Math.sin(bAngle - aAngle);
            pointVector2.p1.y = ct1.center.y - Math.cos(bAngle - aAngle);
            pointVector2.p2.x = ct1.center.x + Math.sin(bAngle + aAngle);
            pointVector2.p2.y = ct1.center.y - Math.cos(bAngle - aAngle);
            return pointVector2;
        }

        return pointVector2;
    }

    // 获取三个点的中心点
    public static Point getCenterOfThreePoint(Point p1, Point p2, Point p3) {
        return new Point((p1.x + p2.x + p3.x) / 3, (p1.y + p2.y + p3.y) / 3);
    }
}
