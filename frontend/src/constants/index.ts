// 判题状态 -> 文案与标签颜色
export const JUDGE_STATUS: Record<string, { text: string; color: string }> = {
  PENDING: { text: '等待判题', color: 'default' },
  JUDGING: { text: '判题中', color: 'processing' },
  ACCEPTED: { text: '通过', color: 'success' },
  WRONG_ANSWER: { text: '答案错误', color: 'error' },
  COMPILE_ERROR: { text: '编译错误', color: 'warning' },
  RUNTIME_ERROR: { text: '运行错误', color: 'magenta' },
  TIME_LIMIT_EXCEEDED: { text: '超时', color: 'gold' },
  MEMORY_LIMIT_EXCEEDED: { text: '超内存', color: 'gold' },
  SYSTEM_ERROR: { text: '系统错误', color: 'default' },
}

export const getStatusMeta = (status: string) =>
  JUDGE_STATUS[status] ?? { text: status || '未知', color: 'default' }

// 难度 -> 文案与标签颜色
export const DIFFICULTY: Record<number, { text: string; color: string }> = {
  1: { text: '简单', color: 'success' },
  2: { text: '中等', color: 'gold' },
  3: { text: '困难', color: 'error' },
}

export const getDifficultyMeta = (level?: number) =>
  DIFFICULTY[level ?? 0] ?? { text: '未知', color: 'default' }

// 课程状态
export const COURSE_STATUS: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  published: { text: '已发布', color: 'success' },
  offline: { text: '已下架', color: 'error' },
}

export const getCourseStatusMeta = (status?: string) =>
  COURSE_STATUS[status ?? ''] ?? { text: status || '未知', color: 'default' }

// 课程难度（字符串枚举，与题目的数字难度不同）
export const COURSE_DIFFICULTY: Record<string, { text: string; color: string }> = {
  beginner: { text: '入门', color: 'success' },
  intermediate: { text: '进阶', color: 'gold' },
  advanced: { text: '高级', color: 'error' },
}

export const getCourseDifficultyMeta = (level?: string) =>
  COURSE_DIFFICULTY[level ?? ''] ?? { text: level || '未知', color: 'default' }

// 默认 Java 代码模板
export const DEFAULT_JAVA_CODE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        System.out.println(a + b);
    }
}
`
