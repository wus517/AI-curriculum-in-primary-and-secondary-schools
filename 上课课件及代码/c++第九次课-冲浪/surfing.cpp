#include <iostream>
#include <vector>
#include <string>
#include <conio.h>
#include <windows.h>
#include <ctime>
#include <cstdlib>

using namespace std;

// ===================== ??????? =====================
const int WIDTH = 25;          // ??????
const int HEIGHT = 20;         // ??????
const char EMPTY = ' ';
const char PLAYER = 'A';
const char COIN = '$';
const char OBSTACLE = '#';
const char BORDER = '|';

// ===================== ??????? =====================

// ?????????????
void hideCursor() {
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    CONSOLE_CURSOR_INFO cursorInfo;
    cursorInfo.dwSize = 1;
    cursorInfo.bVisible = FALSE;
    SetConsoleCursorInfo(hOut, &cursorInfo);
}

// ???????????????????????????
void gotoXY(int x, int y) {
    COORD pos;
    pos.X = x;
    pos.Y = y;
    SetConsoleCursorPosition(GetStdHandle(STD_OUTPUT_HANDLE), pos);
}

// ????????????????????
void clearKeyboardBuffer() {
    while (_kbhit()) _getch();
}

// ===================== ?????? =====================
int main() {
    srand((unsigned)time(0));
    hideCursor();

    string cmd;
    cout << "==============================\n";
    cout << "  Console Runner Game\n";
    cout << "==============================\n";
    cout << "Input \"start\" to begin the game: ";
    cin >> cmd;

    if (cmd != "start") {
        cout << "You did not input start. Program ended.\n";
        return 0;
    }

    // ????????
    vector<string> board(HEIGHT, string(WIDTH, EMPTY));

    int playerX = WIDTH / 2;
    int playerY = HEIGHT - 3;   // ???????????¦Ë????¦Ã¨À????¡À?????????§¹??

    int score = 0;
    int life = 3;
    bool running = true;
    bool userExit = false;

    // ????????????????????/????????????????
    // ??????????????????????????????????
    while (running) {
        // ===================== ?????? =====================
        if (_kbhit()) {
            int key = _getch();

            // ESC ???
            if (key == 27) {
                userExit = true;
                break;
            }

            // A / a ????
            if (key == 'a' || key == 'A') {
                if (playerX > 0) playerX--;
            }
                // D / d ????
            else if (key == 'd' || key == 'D') {
                if (playerX < WIDTH - 1) playerX++;
            }
        }

        // ===================== ??????? =====================
        // ??????????§µ??????????????¡À???
        for (int i = 0; i < HEIGHT - 1; i++) {
            board[i] = board[i + 1];
        }

        // ?????????§Ö????
        string newRow(WIDTH, EMPTY);

        // ???????????????
        // ???????????????????
        for (int j = 0; j < WIDTH; j++) {
            int r = rand() % 100;

            if (r < 8) {
                newRow[j] = OBSTACLE;   // 8% ?????????
            } else if (r < 13) {
                newRow[j] = COIN;       // 5% ??????
            } else {
                newRow[j] = EMPTY;
            }
        }

        board[HEIGHT - 1] = newRow;

        // ===================== ????§Ø? =====================
        if (board[playerY][playerX] == COIN) {
            score += 10;
            board[playerY][playerX] = EMPTY;
        } else if (board[playerY][playerX] == OBSTACLE) {
            life--;
            board[playerY][playerX] = EMPTY;

            if (life <= 0) {
                running = false;
            }
        }

        // ===================== ??????? =====================
        gotoXY(0, 0);

        cout << "Score: " << score << "    Life: " << life
             << "    Control: A/D move, ESC quit\n";
        cout << string(WIDTH + 2, '=') << "\n";

        for (int i = 0; i < HEIGHT; i++) {
            cout << BORDER;
            for (int j = 0; j < WIDTH; j++) {
                if (i == playerY && j == playerX) {
                    cout << PLAYER;
                } else {
                    cout << board[i][j];
                }
            }
            cout << BORDER << "\n";
        }

        cout << string(WIDTH + 2, '=') << "\n";
        cout << "Collect $ for +10 score. Avoid # or lose 1 life.\n";

        // ===================== ?????? =====================
        Sleep(500); // ????§³??????
    }

    gotoXY(0, HEIGHT + 5);

    if (userExit) {
        cout << "\nYou exited the game by pressing ESC.\n";
    } else {
        cout << "\nGame Over!\n";
    }

    cout << "Final Score: " << score << "\n";
    cout << "Remaining Life: " << life << "\n";

    system("pause");
    return 0;
}