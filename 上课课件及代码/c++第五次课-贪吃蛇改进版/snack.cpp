#include <iostream>
#include <vector>
#include <ctime>
#include <cstdlib>
#include <conio.h>
#include <windows.h>

using namespace std;

struct Point {
    int x, y;
};

class SnakeGame {
private:
    const int width = 20;
    const int height = 20;

    vector<Point> snake;
    Point food;
    char direction;   // W A S D
    bool gameOver;
    int score;

public:
    SnakeGame() {
        init();
    }

    void init() {
        snake.clear();
        snake.push_back({width / 2, height / 2});   // 蛇头初始位置
        snake.push_back({width / 2 - 1, height / 2});
        snake.push_back({width / 2 - 2, height / 2});

        direction = 'D';   // 初始向右
        gameOver = false;
        score = 0;

        srand((unsigned)time(0));
        generateFood();
    }

    void generateFood() {
        while (true) {
            food.x = rand() % (width - 2) + 1;   // 避开墙壁
            food.y = rand() % (height - 2) + 1;

            bool onSnake = false;
            for (const auto& part : snake) {
                if (part.x == food.x && part.y == food.y) {
                    onSnake = true;
                    break;
                }
            }

            if (!onSnake) break;
        }
    }

    void draw() {
        system("cls");  // Windows清屏

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 画墙
                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    cout << "#";
                }
                    // 画食物
                else if (x == food.x && y == food.y) {
                    cout << "*";
                }
                else {
                    bool printed = false;

                    // 蛇头
                    if (x == snake[0].x && y == snake[0].y) {
                        cout << "O";
                        printed = true;
                    }
                    else {
                        // 蛇身
                        for (int i = 1; i < snake.size(); i++) {
                            if (x == snake[i].x && y == snake[i].y) {
                                cout << "o";
                                printed = true;
                                break;
                            }
                        }
                    }

                    if (!printed) {
                        cout << " ";
                    }
                }
            }
            cout << endl;
        }

        cout << "Score: " << score << endl;
        cout << "Use W A S D to move, press X to quit." << endl;
    }

    void input() {
        if (_kbhit()) {
            char key = _getch();
            key = toupper(key);

            // 防止蛇直接反向
            if (key == 'W' && direction != 'S') direction = 'W';
            else if (key == 'S' && direction != 'W') direction = 'S';
            else if (key == 'A' && direction != 'D') direction = 'A';
            else if (key == 'D' && direction != 'A') direction = 'D';
            else if (key == 'X') gameOver = true;
        }
    }

    void logic() {
        Point newHead = snake[0];

        switch (direction) {
            case 'W': newHead.y--; break;
            case 'S': newHead.y++; break;
            case 'A': newHead.x--; break;
            case 'D': newHead.x++; break;
        }

        // 撞墙
        if (newHead.x == 0 || newHead.x == width - 1 ||
            newHead.y == 0 || newHead.y == height - 1) {
            gameOver = true;
            return;
        }

        // 撞自己
        for (const auto& part : snake) {
            if (newHead.x == part.x && newHead.y == part.y) {
                gameOver = true;
                return;
            }
        }

        // 头插入到最前面
        snake.insert(snake.begin(), newHead);

        // 吃到食物
        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10;
            generateFood();
        } else {
            // 没吃到则去掉尾巴
            snake.pop_back();
        }
    }

    void run() {
        while (!gameOver) {
            draw();
            input();
            logic();
            Sleep(150);  // 控制游戏速度
        }

        system("cls");
        cout << "Game Over!" << endl;
        cout << "Final Score: " << score << endl;
    }
};

int main() {
    SnakeGame game;
    game.run();
    return 0;
}