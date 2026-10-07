#include <iostream>
#include <windows.h>
using namespace std;

const int ROW = 10;
const int COL = 20;

int main() {
    char map[ROW][COL];

    // ≥ı ºªØµÿÕº
    for (int i = 0; i < ROW; i++) {
        for (int j = 0; j < COL; j++) {
            if (i == 0 || i == ROW - 1 || j == 0 || j == COL - 1)
                map[i][j] = '#';
            else
                map[i][j] = ' ';
        }
    }

    int x[100] = {3, 3, 3};
    int y[100] = {3, 2, 1};
    int len = 3;

    int foodX = 3, foodY = 10;
    map[foodX][foodY] = '*';

    int dx = 0, dy = 1; 

    while (true) {
        int newX = x[0] + dx;
        int newY = y[0] + dy;

        if (map[newX][newY] == '#') {
            cout << "Game Over!" << endl;
            break;
        }

        bool eatFood = (newX == foodX && newY == foodY);

        if (!eatFood) {
            map[x[len - 1]][y[len - 1]] = ' ';
        } else {
            len++;
        }

        for (int i = len - 1; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        x[0] = newX;
        y[0] = newY;

        for (int i = 0; i < len; i++) {
            map[x[i]][y[i]] = 'O';
        }

        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                cout << map[i][j];
            }
            cout << endl;
        }
        cout << endl;

        Sleep(800); // —”≥Ÿ
    }

    return 0;
}
