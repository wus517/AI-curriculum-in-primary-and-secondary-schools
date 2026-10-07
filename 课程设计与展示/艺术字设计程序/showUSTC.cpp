#include <cctype>
#include <iostream>
#include <limits>
#include <string>
#include <windows.h>

using namespace std;

// 一共支持 26 个大写字母和 10 个数字。
const int FONT_COUNT = 36;

// 每个基础字符模板占 5 行。
const int FONT_HEIGHT = 5;

// 为放大后的艺术字预留最多 20 行空间。
const int MAX_LINES = 20;

// 用固定宽度来做居中显示。
const int CONSOLE_WIDTH = 100;

// 按顺序保存所有支持的字符，方便后面查找模板位置。
const string FONT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

// 下面这些变量保存程序当前的设置。
string currentText = "USTC";
int currentSize = 1;
char currentFillChar = '*';
char currentBorderChar = '#';
int currentTextColor = 14;
int currentBorderColor = 11;

// FONT 是一个二维数组。
// 每个字符都有 5 行字符串模板，X 表示要画出来的位置，空格表示留白。
const string FONT[FONT_COUNT][FONT_HEIGHT] = {
    {" XXX ", "X   X", "XXXXX", "X   X", "X   X"},
    {"XXXX ", "X   X", "XXXX ", "X   X", "XXXX "},
    {" XXX ", "X   X", "X    ", "X   X", " XXX "},
    {"XXXX ", "X   X", "X   X", "X   X", "XXXX "},
    {"XXXXX", "X    ", "XXXX ", "X    ", "XXXXX"},
    {"XXXXX", "X    ", "XXXX ", "X    ", "X    "},
    {" XXX ", "X    ", "X XXX", "X   X", " XXXX"},
    {"X   X", "X   X", "XXXXX", "X   X", "X   X"},
    {"XXXXX", "  X  ", "  X  ", "  X  ", "XXXXX"},
    {"XXXXX", "   X ", "   X ", "X  X ", " XX  "},
    {"X   X", "X  X ", "XXX  ", "X  X ", "X   X"},
    {"X    ", "X    ", "X    ", "X    ", "XXXXX"},
    {"X   X", "XX XX", "X X X", "X   X", "X   X"},
    {"X   X", "XX  X", "X X X", "X  XX", "X   X"},
    {" XXX ", "X   X", "X   X", "X   X", " XXX "},
    {"XXXX ", "X   X", "XXXX ", "X    ", "X    "},
    {" XXX ", "X   X", "X   X", "X  XX", " XXXX"},
    {"XXXX ", "X   X", "XXXX ", "X  X ", "X   X"},
    {" XXXX", "X    ", " XXX ", "    X", "XXXX "},
    {"XXXXX", "  X  ", "  X  ", "  X  ", "  X  "},
    {"X   X", "X   X", "X   X", "X   X", " XXX "},
    {"X   X", "X   X", "X   X", " X X ", "  X  "},
    {"X   X", "X   X", "X X X", "XX XX", "X   X"},
    {"X   X", " X X ", "  X  ", " X X ", "X   X"},
    {"X   X", " X X ", "  X  ", "  X  ", "  X  "},
    {"XXXXX", "   X ", "  X  ", " X   ", "XXXXX"},
    {" XXX ", "X  XX", "X X X", "XX  X", " XXX "},
    {"  X  ", " XX  ", "  X  ", "  X  ", "XXXXX"},
    {" XXX ", "X   X", "   X ", "  X  ", "XXXXX"},
    {" XXX ", "    X", " XXX ", "    X", " XXX "},
    {"X   X", "X   X", "XXXXX", "    X", "    X"},
    {"XXXXX", "X    ", "XXXX ", "    X", "XXXX "},
    {" XXX ", "X    ", "XXXX ", "X   X", " XXX "},
    {"XXXXX", "   X ", "  X  ", " X   ", "X    "},
    {" XXX ", "X   X", " XXX ", "X   X", " XXX "},
    {" XXX ", "X   X", " XXXX", "    X", " XXX "}
};

// 清空输入错误状态，并把输入缓冲区里剩余内容清掉。
void clearInputState() {
    cin.clear();
    cin.ignore(numeric_limits<streamsize>::max(), '\n');
}

// 设置控制台当前输出颜色。
void setColor(int color) {
    SetConsoleTextAttribute(GetStdHandle(STD_OUTPUT_HANDLE), (WORD)color);
}

// 恢复默认颜色，7 通常表示白色或默认浅灰色。
void resetColor() {
    setColor(7);
}

// 把用户输入的字符串统一转换成大写，便于和模板对应。
string toUpperText(string text) {
    for (int i = 0; i < (int)text.size(); i++) {
        text[i] = (char)toupper((unsigned char)text[i]);
    }
    return text;
}

// 检查输入内容是否合法：
// 1. 不能为空
// 2. 只能包含字母和数字
bool isValidText(string text) {
    if (text.size() == 0) {
        return false;
    }

    for (int i = 0; i < (int)text.size(); i++) {
        if (!isalnum((unsigned char)text[i])) {
            return false;
        }
    }
    return true;
}

// 在 FONT_CHARS 里查找字符位置。
// 例如找到 'A'，就返回它对应的下标。
int findCharIndex(char ch) {
    for (int i = 0; i < FONT_COUNT; i++) {
        if (FONT_CHARS[i] == ch) {
            return i;
        }
    }
    return -1;
}

// 显示主菜单。
void showMenu() {
    cout << "\n========== USTC 艺术字程序 ==========\n";
    cout << "1. 生成图案\n";
    cout << "2. 修改设置\n";
    cout << "3. 预览\n";
    cout << "4. 查看设置\n";
    cout << "5. 退出\n";
    cout << "======================================\n";
}

// 获取主菜单选项，只允许输入 1 到 5。
int getMenuChoice() {
    int choice;
    while (true) {
        cout << "请选择功能（1-5）: ";
        if (cin >> choice && choice >= 1 && choice <= 5) {
            clearInputState();
            return choice;
        }
        cout << "输入错误，请重新输入。\n";
        clearInputState();
    }
}

// 获取艺术字大小，只允许输入 1、2、3。
int getSizeInput() {
    int size;
    while (true) {
        cout << "请选择大小（1=小，2=中，3=大）: ";
        if (cin >> size && size >= 1 && size <= 3) {
            clearInputState();
            return size;
        }
        cout << "输入错误，请重新输入。\n";
        clearInputState();
    }
}

// 获取单个字符输入，例如字母字符或边框字符。
char getSingleCharInput(string prompt) {
    string input;
    while (true) {
        cout << prompt;
        getline(cin, input);
        if (input.size() == 1) {
            return input[0];
        }
        cout << "请输入且只输入一个字符。\n";
    }
}

// 获取颜色编号，只允许输入 0 到 15。
int getColorInput(string prompt) {
    int color;
    while (true) {
        cout << prompt << "\n";
        cout << "0=黑 1=蓝 2=绿 3=青 4=红 5=紫 6=黄 7=白\n";
        cout << "8=灰 9=亮蓝 10=亮绿 11=亮青 12=亮红 13=亮紫 14=亮黄 15=亮白\n";
        cout << "请输入颜色编号: ";
        if (cin >> color && color >= 0 && color <= 15) {
            clearInputState();
            return color;
        }
        cout << "输入错误，请重新输入。\n";
        clearInputState();
    }
}

// “修改设置”子菜单。
int getSettingChoice() {
    int choice;
    while (true) {
        cout << "\n----- 修改设置 -----\n";
        cout << "1. 修改文字内容\n";
        cout << "2. 修改大小\n";
        cout << "3. 修改字母字符\n";
        cout << "4. 修改边框字符\n";
        cout << "5. 修改字母颜色\n";
        cout << "6. 修改边框颜色\n";
        cout << "7. 返回上一级\n";
        cout << "--------------------\n";
        cout << "请选择要修改的项目（1-7）: ";

        if (cin >> choice && choice >= 1 && choice <= 7) {
            clearInputState();
            return choice;
        }

        cout << "输入错误，请重新输入。\n";
        clearInputState();
    }
}

// 显示当前设置，方便用户查看现在的程序状态。
void showSettings() {
    cout << "\n----- 当前设置 -----\n";
    cout << "文字内容: " << currentText << "\n";
    cout << "大小: " << currentSize << "\n";
    cout << "字母字符: " << currentFillChar << "\n";
    cout << "边框字符: " << currentBorderChar << "\n";
    cout << "字母颜色: " << currentTextColor << "\n";
    cout << "边框颜色: " << currentBorderColor << "\n";
    cout << "显示宽度: " << CONSOLE_WIDTH << "\n";
    cout << "--------------------\n";
}

// 生成单个字符的艺术字图案。
// result[] 用来保存生成后的多行字符串。
// lineCount 会告诉外面一共生成了多少行。
void getOneCharPattern(char ch, string result[], int& lineCount) {
    int index = findCharIndex(ch);
    lineCount = FONT_HEIGHT * currentSize;

    // 先把结果数组清空。
    for (int i = 0; i < lineCount; i++) {
        result[i] = "";
    }

    // 如果没找到对应模板，就直接返回空结果。
    if (index == -1) {
        return;
    }

    // 逐行处理字符模板。
    for (int row = 0; row < FONT_HEIGHT; row++) {
        string newLine = "";

        // 逐列读取这一行里的每个字符。
        for (int col = 0; col < (int)FONT[index][row].size(); col++) {
            char now = FONT[index][row][col];

            // 如果当前位置是 X，说明这里要画出来，
            // 就把它换成用户当前选择的字符。
            if (now == 'X') {
                now = currentFillChar;
            }

            // 横向放大：把当前字符重复 currentSize 次。
            for (int k = 0; k < currentSize; k++) {
                newLine += now;
            }
        }

        // 纵向放大：把这一整行也重复 currentSize 次。
        for (int k = 0; k < currentSize; k++) {
            result[row * currentSize + k] = newLine;
        }
    }
}

// 把整段文字渲染成艺术字。
// 做法是：一个字符一个字符生成，再按行拼接起来。
void renderText(string result[], int& lineCount) {
    string oneChar[MAX_LINES];
    int oneCharLines = 0;
    int gap = currentSize;

    lineCount = FONT_HEIGHT * currentSize;

    // 先把结果数组清空。
    for (int i = 0; i < lineCount; i++) {
        result[i] = "";
    }

    // 逐个处理用户输入的字符。
    for (int i = 0; i < (int)currentText.size(); i++) {
        getOneCharPattern(currentText[i], oneChar, oneCharLines);

        // 把当前字符的每一行拼接到总结果上。
        for (int row = 0; row < lineCount; row++) {
            if (i > 0) {
                for (int s = 0; s < gap; s++) {
                    result[row] += ' ';
                }
            }
            result[row] += oneChar[row];
        }
    }
}

// 找出多行字符串中最长的一行宽度。
int getMaxWidth(string lines[], int lineCount) {
    int maxWidth = 0;
    for (int i = 0; i < lineCount; i++) {
        if ((int)lines[i].size() > maxWidth) {
            maxWidth = (int)lines[i].size();
        }
    }
    return maxWidth;
}

// 为艺术字内容加上矩形边框。
void addBorder(string inputLines[], int inputCount, string outputLines[], int& outputCount) {
    int width = getMaxWidth(inputLines, inputCount);
    string topBottom = "";

    // 先生成上下边框。
    for (int i = 0; i < width + 4; i++) {
        topBottom += currentBorderChar;
    }

    outputCount = inputCount + 2;
    outputLines[0] = topBottom;

    // 给中间每一行左右两边加上边框字符。
    for (int i = 0; i < inputCount; i++) {
        string line = inputLines[i];

        // 如果某一行比最长宽度短，就补空格对齐。
        while ((int)line.size() < width) {
            line += ' ';
        }

        outputLines[i + 1] = "";
        outputLines[i + 1] += currentBorderChar;
        outputLines[i + 1] += ' ';
        outputLines[i + 1] += line;
        outputLines[i + 1] += ' ';
        outputLines[i + 1] += currentBorderChar;
    }

    outputLines[outputCount - 1] = topBottom;
}

// 通过在左边补空格，让图案整体居中。
void addCenterPadding(string inputLines[], int lineCount, string outputLines[]) {
    int width = getMaxWidth(inputLines, lineCount);
    int leftPadding = 0;

    if (CONSOLE_WIDTH > width) {
        leftPadding = (CONSOLE_WIDTH - width) / 2;
    }

    for (int i = 0; i < lineCount; i++) {
        outputLines[i] = "";
        for (int j = 0; j < leftPadding; j++) {
            outputLines[i] += ' ';
        }
        outputLines[i] += inputLines[i];
    }
}

// 按颜色输出最终结果。
// 边框和艺术字内容使用不同颜色。
void printColoredLines(string lines[], int lineCount) {
    int indent = -1;
    int width = 0;

    // 先找出整块图案真正开始的位置和宽度。
    for (int i = 0; i < lineCount; i++) {
        int first = -1;
        for (int j = 0; j < (int)lines[i].size(); j++) {
            if (lines[i][j] != ' ') {
                first = j;
                break;
            }
        }

        if (first != -1) {
            if (indent == -1 || first < indent) {
                indent = first;
            }
            if ((int)lines[i].size() - first > width) {
                width = (int)lines[i].size() - first;
            }
        }
    }

    if (indent == -1) {
        indent = 0;
    }

    // 逐行逐字符输出。
    for (int row = 0; row < lineCount; row++) {
        for (int col = 0; col < (int)lines[row].size(); col++) {
            char ch = lines[row][col];

            if (ch == ' ') {
                resetColor();
                cout << ch;
            } else {
                bool isBorder = false;

                // 第一行、最后一行、最左边和最右边属于边框。
                if (row == 0 || row == lineCount - 1 || col == indent || col == indent + width - 1) {
                    isBorder = true;
                }

                if (isBorder) {
                    setColor(currentBorderColor);
                } else {
                    setColor(currentTextColor);
                }
                cout << ch;
            }
        }
        resetColor();
        cout << "\n";
    }
}

// 把“渲染文字、加边框、居中、打印”这几步串起来。
void buildAndPrintArt() {
    string artLines[MAX_LINES];
    string borderLines[MAX_LINES + 2];
    string finalLines[MAX_LINES + 2];
    int artCount = 0;
    int borderCount = 0;

    renderText(artLines, artCount);
    addBorder(artLines, artCount, borderLines, borderCount);
    addCenterPadding(borderLines, borderCount, finalLines);
    printColoredLines(finalLines, borderCount);
}

// 预览当前设置下的输出效果。
void previewArt() {
    cout << "\n预览效果:\n";
    buildAndPrintArt();
}

// 修改设置。
// 这里使用子菜单，让用户可以一次只改一项。
void changeSettings() {
    string inputText;
    bool editing = true;

    while (editing) {
        showSettings();
        int choice = getSettingChoice();

        if (choice == 1) {
            while (true) {
                cout << "请输入文字内容（只支持字母和数字）: ";
                getline(cin, inputText);
                inputText = toUpperText(inputText);

                if (isValidText(inputText)) {
                    currentText = inputText;
                    cout << "文字内容修改完成。\n";
                    previewArt();
                    break;
                }

                cout << "输入内容不合法，请重新输入。\n";
            }
        } else if (choice == 2) {
            currentSize = getSizeInput();
            cout << "大小修改完成。\n";
            previewArt();
        } else if (choice == 3) {
            currentFillChar = getSingleCharInput("请输入字母字符: ");
            cout << "字母字符修改完成。\n";
            previewArt();
        } else if (choice == 4) {
            currentBorderChar = getSingleCharInput("请输入边框字符: ");
            cout << "边框字符修改完成。\n";
            previewArt();
        } else if (choice == 5) {
            currentTextColor = getColorInput("请选择字母颜色");
            cout << "字母颜色修改完成。\n";
            previewArt();
        } else if (choice == 6) {
            currentBorderColor = getColorInput("请选择边框颜色");
            cout << "边框颜色修改完成。\n";
            previewArt();
        } else if (choice == 7) {
            editing = false;
        }
    }
}

// 主循环：不断显示菜单并执行用户选择的功能。
void runProgram() {
    bool running = true;

    while (running) {
        showMenu();
        int choice = getMenuChoice();

        if (choice == 1) {
            cout << "\n生成结果:\n";
            buildAndPrintArt();
        } else if (choice == 2) {
            changeSettings();
        } else if (choice == 3) {
            previewArt();
        } else if (choice == 4) {
            showSettings();
        } else if (choice == 5) {
            running = false;
        }
    }
}

// 程序入口。
int main() {
	
    runProgram();
    resetColor();
    return 0;
}

