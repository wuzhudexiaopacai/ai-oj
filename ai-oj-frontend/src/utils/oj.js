/**
 * 判题状态 / 题目难度 / 语言 等字典
 * 与后端 JudgeStatusEnum、ProblemDifficultyEnum 编码保持一致
 */

// JudgeStatusEnum：0 排队中 1 编译中 2 评测中 3 AC 4 WA 5 TLE 6 MLE 7 RE 8 CE 9 PE 10 OLE 11 SE
export const JUDGE_STATUS = {
  0: { key: 'PENDING', desc: '排队中', type: 'info' },
  1: { key: 'COMPILING', desc: '编译中', type: 'info' },
  2: { key: 'JUDGING', desc: '评测中', type: 'info' },
  3: { key: 'ACCEPTED', desc: '答案正确', type: 'success' },
  4: { key: 'WRONG_ANSWER', desc: '答案错误', type: 'danger' },
  5: { key: 'TIME_LIMIT_EXCEEDED', desc: '运行超时', type: 'warning' },
  6: { key: 'MEMORY_LIMIT_EXCEEDED', desc: '内存超限', type: 'warning' },
  7: { key: 'RUNTIME_ERROR', desc: '运行错误', type: 'danger' },
  8: { key: 'COMPILE_ERROR', desc: '编译错误', type: 'danger' },
  9: { key: 'PRESENTATION_ERROR', desc: '格式错误', type: 'warning' },
  10: { key: 'OUTPUT_LIMIT_EXCEEDED', desc: '输出超限', type: 'warning' },
  11: { key: 'SYSTEM_ERROR', desc: '系统错误', type: 'info' }
}

/** 非终态状态码集合（用于前端轮询） */
export const NON_TERMINAL_STATUS = [0, 1, 2]

export function judgeStatusOf(code) {
  return JUDGE_STATUS[code] || { key: 'UNKNOWN', desc: '未知', type: 'info' }
}

// ProblemDifficultyEnum：0 简单 1 中等 2 困难
export const DIFFICULTY = {
  0: { key: 'EASY', desc: '简单', color: 'var(--oj-success)' },
  1: { key: 'MEDIUM', desc: '中等', color: 'var(--oj-warning)' },
  2: { key: 'HARD', desc: '困难', color: 'var(--oj-danger)' }
}

export function difficultyOf(code) {
  return DIFFICULTY[code] || { key: 'UNKNOWN', desc: '未知', color: 'var(--oj-info)' }
}

/** 提交语言选项（value 提交给后端，monaco 为编辑器语言） */
export const LANGUAGES = [
  { value: 'cpp', label: 'C++17', monaco: 'cpp' },
  { value: 'c', label: 'C11', monaco: 'c' },
  { value: 'java', label: 'Java 17', monaco: 'java' },
  { value: 'python3', label: 'Python 3', monaco: 'python' },
  { value: 'go', label: 'Go', monaco: 'go' },
  { value: 'javascript', label: 'JavaScript', monaco: 'javascript' }
]

export function languageOf(value) {
  return LANGUAGES.find((l) => l.value === value) || { value, label: value, monaco: 'plaintext' }
}

/** 各语言默认代码模板 */
export const CODE_TEMPLATES = {
  cpp: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n    \n    return 0;\n}\n',
  c: '#include <stdio.h>\n\nint main(void) {\n    \n    return 0;\n}\n',
  java: 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        \n    }\n}\n',
  python3: 'import sys\n\ninput = sys.stdin.readline\n\n',
  go: 'package main\n\nimport "fmt"\n\nfunc main() {\n\t\n}\n',
  javascript: "'use strict';\nconst readline = require('readline');\nconst rl = readline.createInterface({ input: process.stdin });\nconst lines = [];\nrl.on('line', (l) => lines.push(l)).on('close', () => {\n    \n});\n"
}
