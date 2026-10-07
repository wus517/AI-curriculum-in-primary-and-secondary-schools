#include <easyx.h>
#include <cmath>
#include <windows.h>
#include <stdio.h>
#include <time.h>
#include <stdlib.h>

#define PI 3.1415926

// ================= 行星结构 =================
struct Planet
{
    double angle;
    double speed;
    double a, b;
    int size;
    COLORREF color;
    const char* name_cn;
};

// ================= 数据 =================
Planet planets[8] =
{
    {0, 0.05, 100, 60, 6,  RGB(200,200,200), "水星"},
    {0, 0.04, 150, 90, 10, RGB(255,200,100), "金星"},
    {0, 0.03, 200, 120,11, RGB(100,150,255), "地球"},
    {0, 0.025,250, 160,9, RGB(255,100,100), "火星"},
    {0, 0.015,320, 200,20, RGB(200,180,100), "木星"},
    {0, 0.012,380, 240,18, RGB(220,220,180), "土星"},
    {0, 0.008,450, 280,14, RGB(150,220,255), "天王星"},
    {0, 0.006,520, 320,14, RGB(100,100,255), "海王星"}
};

// ================= 全局 =================
double globalSpeed = 1.0;
bool paused = false;
double simDay = 0;
int selected = -1;

// ================= 星星 =================
struct Star { int x, y; };
Star stars[200];

void InitStars()
{
    for (int i = 0; i < 200; i++)
    {
        stars[i].x = rand() % 1000 - 500;
        stars[i].y = rand() % 800 - 400;
    }
}

void DrawStars()
{
    setfillcolor(WHITE);
    for (int i = 0; i < 200; i++)
        solidcircle(stars[i].x, stars[i].y, 1);
}

// ================= 绘制系统 =================
void DrawSystem()
{
    setorigin(getwidth() / 2, getheight() / 2); // 原点在中心
    setaspectratio(1,1); // 保证 X/Y 比例
    cleardevice();

    DrawStars();

    // 绘制太阳
    setfillcolor(YELLOW);
    solidcircle(0, 0, 40);

    // 绘制行星
    for (int i = 0; i < 8; i++)
    {
        setlinecolor(RGB(60,60,60));
        ellipse(-(int)planets[i].a, -(int)planets[i].b,
                 (int)planets[i].a, (int)planets[i].b);

        double x = planets[i].a * cos(planets[i].angle);
        double y = planets[i].b * sin(planets[i].angle);

        setfillcolor(planets[i].color);
        solidcircle((int)x, (int)y, planets[i].size);

        settextcolor(WHITE);
        outtextxy((int)x + 8, (int)y + 8, planets[i].name_cn);

        if (i == selected)
        {
            setlinecolor(WHITE);
            circle((int)x, (int)y, planets[i].size + 5);
        }
    }

    // ================= UI =================
    settextcolor(WHITE);
    char buf[128];

    sprintf(buf, "速度: x%.2f", globalSpeed);
    outtextxy(-450, -350, buf);

    sprintf(buf, "模拟时间: %.0f 天", simDay);
    outtextxy(-450, -320, buf);

    outtextxy(-450, -290, paused ? "状态: 暂停" : "状态: 运行");

    // ================= 行星信息面板 =================
    if (selected != -1)
    {
        int x0 = -450;
        int y0 = -250;

        setfillcolor(RGB(30, 30, 30));
        solidrectangle(x0, y0, x0 + 300, y0 + 180);

        setlinecolor(WHITE);
        rectangle(x0, y0, x0 + 300, y0 + 180);

        settextcolor(WHITE);
        outtextxy(x0 + 10, y0 + 10, "=== 行星信息 ===");
        outtextxy(x0 + 10, y0 + 40, planets[selected].name_cn);

        if (selected == 0)
        {
            outtextxy(x0 + 10, y0 + 70, "距离太阳最近");
            outtextxy(x0 + 10, y0 + 90, "昼夜温差极大");
            outtextxy(x0 + 10, y0 + 110, "没有大气保护");
        }
        else if (selected == 1)
        {
            outtextxy(x0 + 10, y0 + 70, "最炽热行星");
            outtextxy(x0 + 10, y0 + 90, "厚重二氧化碳大气");
            outtextxy(x0 + 10, y0 + 110, "表面高温极端环境");
        }
        else if (selected == 2)
        {
            outtextxy(x0 + 10, y0 + 70, "唯一存在生命");
            outtextxy(x0 + 10, y0 + 90, "拥有液态水");
            outtextxy(x0 + 10, y0 + 110, "有一颗月球");
        }
        else if (selected == 3)
        {
            outtextxy(x0 + 10, y0 + 70, "红色星球");
            outtextxy(x0 + 10, y0 + 90, "可能存在水冰");
            outtextxy(x0 + 10, y0 + 110, "未来探测目标");
        }
        else
        {
            outtextxy(x0 + 10, y0 + 70, "气态/冰质行星");
            outtextxy(x0 + 10, y0 + 90, "体积巨大");
            outtextxy(x0 + 10, y0 + 110, "拥有众多卫星");
        }
    }
}

// ================= 更新 =================
void Update()
{
    if (!paused)
    {
        for (int i = 0; i < 8; i++)
            planets[i].angle += planets[i].speed * globalSpeed;

        simDay += globalSpeed;
    }
}

// ================= 输入 =================
void HandleInput()
{
    ExMessage msg;

    while (peekmessage(&msg))
    {
        if (msg.message == WM_LBUTTONDOWN)
        {
            int cx = getwidth() / 2;
            int cy = getheight() / 2;

            double mx = msg.x - cx;
            double my = msg.y - cy;

            selected = -1;

            for (int i = 0; i < 8; i++)
            {
                double x = planets[i].a * cos(planets[i].angle);
                double y = planets[i].b * sin(planets[i].angle);

                double dist = sqrt((mx - x)*(mx - x) + (my - y)*(my - y));

                if (dist < planets[i].size + 5)
                    selected = i;
            }
        }

        if (msg.message == WM_KEYDOWN)
        {
            if (msg.vkcode == VK_SPACE) paused = !paused;
            if (msg.vkcode == VK_UP) globalSpeed *= 1.2;
            if (msg.vkcode == VK_DOWN) globalSpeed /= 1.2;
            if (msg.vkcode == VK_ESCAPE) exit(0);
        }
    }
}

// ================= 主函数 =================
int main()
{
    initgraph(1000, 800);  // 创建窗口

    srand((unsigned)time(NULL));
    InitStars();

    BeginBatchDraw(); // 批量绘制

    while (true)
    {
        HandleInput();
        Update();
        DrawSystem();

        FlushBatchDraw();
        Sleep(10);
    }

    EndBatchDraw();
    closegraph();
    return 0;
}
