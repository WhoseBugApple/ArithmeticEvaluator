package arithmetric.dfs.standard;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * <h1>Intent</h1>
 * <p> evaluate a String arithmetic expression, all number will be recognized as double </p>
 *
 * <h1>What is dfs</h1>
 * <p> 1*2 + 3*4*5 + 6*7 </p>
 * <p> level 1, + operation, ()  + ()  + () </p>
 * <p> level 2, * operation, 1*2, 3*4*5, 6*7 </p>
 * @Author KORC 792715299@qq.com
 * @Date 2023/6/9
 */
public class DFSArithmeticEvaluator {
    ParenthesisManager pm = new ParenthesisManager();
    Scanner scanner;
    ScannerUtils scannerUtils = new ScannerUtils();
    Operators ops = new Operators();

    public double compute(String expression) {
        double computed = -1;
        try {
            scanner = new Scanner(scannerUtils.getSplitExpression(expression, ops, pm));
            computed = getComputedBetweenParentheses(true);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            return computed;
        }
    }

    private double getComputedBetweenParentheses(boolean isGlobalComputation) {
        if (!isGlobalComputation) {
            // assert next is left parenthesis
            if (!scannerUtils.hasNextLeftParenthesis(scanner, pm))
                throw new RuntimeException("expect a (, while not found");
            scannerUtils.nextLeftParenthesis(scanner, pm);
            pm.push();
        }

        double ret = computeGELevel(ops.lowestOperatorLevel());

        if (!isGlobalComputation) {
            // assert next is right parenthesis
            if (!scannerUtils.hasNextRightParenthesis(scanner, pm))
                throw new RuntimeException("expect a ), while not found");
            scannerUtils.nextRightParenthesis(scanner, pm);
            pm.pop();
        } else {
            // assert no more computations
            if (scannerUtils.hasNextRightParenthesis(scanner, pm))
                throw new RuntimeException("unexpected )");
            if (scanner.hasNext("[^\\s]*"))
                throw new RuntimeException("computation error");
            if (pm.depth() != 0)
                throw new RuntimeException("missing " + pm.depth() + " trailing ) at the end of expression");
        }
        return ret;
    }

    // any equal or higher level computation is computed
    // compute in level */
    // ... + a * () / ++b * c + ... is computed, + is not computed because it's a lower level
    private double computeGELevel(int level) {
        if (level > ops.highestOperatorLevel()) {
            if (scannerUtils.hasNextParenthesis(scanner, pm)) {
                if (scannerUtils.hasNextLeftParenthesis(scanner, pm))
                    return getComputedBetweenParentheses(false);
                else throw new RuntimeException("unexpected )");
            }
            return scannerUtils.nextDouble(scanner);
        }

        int higherLevel = level + 1;
        double ret = computeGELevel(higherLevel);
        while(continueComputation(level)) {
            // assert the op's level is equal to current level
            char op = scannerUtils.nextOperator(scanner, ops);
            ret = ops.primitiveComputation(ret, computeGELevel(higherLevel), op);
        }
        return ret;
    }

    private boolean continueComputation(int currentLevel) {
        if (!scannerUtils.hasNext(scanner)) return false;
        // has sth, maybe () num op
        if (scannerUtils.hasNextParenthesis(scanner, pm)) {
            if (scannerUtils.hasNextRightParenthesis(scanner, pm)) return false;
            else throw new RuntimeException("unexpected (");
        }
        // not ()
        if (scannerUtils.hasNextOperator(scanner, ops)) {
            char op = scannerUtils.peekOperator(scanner, ops);
            return ops.operatorLevel(op) >= currentLevel;
        }
        // not op
        // is num
        throw new RuntimeException("unexpected number");
    }
}


class Operators {
    public boolean isOperator(char op) {
        switch (op) {
            case '+':
            case '-':
            case '*':
            case '/':
                return true;
        }
        return false;
    }

    public double primitiveComputation(double n1, double n2, char op) {
        double ret = 0;
        switch (op) {
            case '+':
                ret = n1 + n2;
                break;
            case '-':
                ret = n1 - n2;
                break;
            case '*':
                ret = n1 * n2;
                break;
            case '/':
                ret = n1 / n2;
                break;
        }
        return ret;
    }

    // [C++ Operator Precedence - cppreference](https://en.cppreference.com/w/cpp/language/operator_precedence)
    public int operatorLevel(char op) {
        int ret = -1;
        switch (op) {
            case '+':
            case '-':
                ret = 1;
                break;
            case '*':
            case '/':
                ret = 2;
                break;
        }
        return ret;
    }

    // [C++ Operator Precedence - cppreference](https://en.cppreference.com/w/cpp/language/operator_precedence)
    public int lowestOperatorLevel() {
        return 1;
    }

    // [C++ Operator Precedence - cppreference](https://en.cppreference.com/w/cpp/language/operator_precedence)
    public int highestOperatorLevel() {
        return 17;
    }

    public Pattern regexp() {
        return Pattern.compile("[+\\-*/]");
    }
}


class ParenthesisManager {
    int count = 0;

    public void push() {
        count++;
    }

    public void pop() {
        count--;
        if (count < 0) throw new RuntimeException("parenthesis count < 0");
    }

    public int getCount() {return count;}
    public int count() {return getCount();}
    public int depth() {return getCount();}

    public char getLeftParenthesis() {
        return '(';
    }

    public char getRightParenthesis() {
        return ')';
    }

    public Pattern leftRegexp() {
        return Pattern.compile("\\(");
    }

    public Pattern rightRegexp() {
        return Pattern.compile("\\)");
    }

    public Pattern regexp() {
        return Pattern.compile("[()]");
    }
}


class ScannerUtils {
    // split elements with whitespace
    public String getSplitExpression(String expression, Operators ops, ParenthesisManager pm) {
        Pattern opPat = ops.regexp();
        Pattern pPat = pm.regexp();
        Pattern numPat = Pattern.compile("[0-9.]");
        StringBuilder builder = new StringBuilder();
        char[] chars = expression.toCharArray();
        int charCursor = 0;

        while(true) {
            // first char of substring in current loop
            if (charCursor >= chars.length) break;
            char c0 = chars[charCursor];
            String s0 = String.valueOf(c0);

            if (opPat.matcher(s0).matches()) {
                builder.append(" ");
                builder.append(c0);
                builder.append(" ");
                charCursor++;
            } else if (pPat.matcher(s0).matches()) {
                builder.append(" ");
                builder.append(c0);
                builder.append(" ");
                charCursor++;
            } else if (numPat.matcher(s0).matches()) {
                builder.append(" ");
                builder.append(c0);

                // try to append all remain chars of number
                while(true) {
                    charCursor++;
                    if (charCursor >= chars.length) break;
                    char c = chars[charCursor];
                    String s = String.valueOf(c);
                    if (!numPat.matcher(s).matches()) break;

                    builder.append(c);
                }

                builder.append(" ");
            } else {
                builder.append(c0);
                charCursor++;
            }
        }

        return builder.toString();
    }

    public boolean hasNextParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.hasNext(pm.regexp());
    }

    public char nextParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.next(pm.regexp()).charAt(0);
    }

    public char peekParenthesis(Scanner scanner, ParenthesisManager pm) {
        scanner.hasNext(pm.regexp());
        return scanner.match().group(0).charAt(0);
    }

    public boolean hasNextLeftParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.hasNext(pm.leftRegexp());
    }

    public char nextLeftParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.next(pm.leftRegexp()).charAt(0);
    }

    public boolean hasNextRightParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.hasNext(pm.rightRegexp());
    }

    public char nextRightParenthesis(Scanner scanner, ParenthesisManager pm) {
        return scanner.next(pm.rightRegexp()).charAt(0);
    }

    public double nextDouble (Scanner scanner) {
        if (scanner.hasNextDouble()) {
            return scanner.nextDouble();
        } else if (scanner.hasNextInt()) {
            return scanner.nextInt();
        } else {
            // throw Exception
            scanner.nextDouble();
            return -1;
        }
    }

    public boolean hasNextOperator(Scanner scanner, Operators ops) {
        return scanner.hasNext(ops.regexp());
    }

    public char nextOperator(Scanner scanner, Operators ops) {
        return scanner.next(ops.regexp()).charAt(0);
    }

    public char peekOperator(Scanner scanner, Operators ops) {
        scanner.hasNext(ops.regexp());
        return scanner.match().group(0).charAt(0);
    }

    public boolean hasNext(Scanner scanner) {
        return scanner.hasNext();
    }
}


class ArithmeticEvaluatorTest {
    String test0 = "1.0 * 2.0 * 3.0 * 4.0 * 5.0 / 2.0 + 3.0 * 4.0 * 5.0 * 6.0 * 7.0 / 3.0";
    double expected0 = 1.0 * 2.0 * 3.0 * 4.0 * 5.0 / 2.0 + 3.0 * 4.0 * 5.0 * 6.0 * 7.0 / 3.0;

    String test1 = "( 1 + 2 - 0 ) * 4 / 3 + 5 * ( 66 + 77 )";
    double expected1 = ( 1 + 2 - 0 ) * 4 / 3 + 5 * ( 66 + 77 );

    String test2 = "( 1 + 22 - 0 ) * 4.0 / 3 + 15 * ( 66 + 77 + 2 * 11 + 101.0 / 2 ) + 99 / 4.0 ";
    double expected2 = ( 1 + 22 - 0 ) * 4.0 / 3 + 15 * ( 66 + 77 + 2 * 11 + 101.0 / 2 ) + 99 / 4.0 ;

    String test3 = "(1+22-0)*4.0/3+15*(66+77+2*11+101.0/2)+99/4.0";
    double expected3 = (1+22-0)*4.0/3+15*(66+77+2*11+101.0/2)+99/4.0 ;

    String test4 = "(1+22-0)*4.0/3+15*(66+(77+2*(11+101.0))/2)+99/4.0";
    double expected4 = (1+22-0)*4.0/3+15*(66+(77+2*(11+101.0))/2)+99/4.0;

    String test5 = "(2)";
    double expected5 = (2);

    String test6 = "((2+3)*(5.0/2))";
    double expected6 = ((2+3)*(5.0/2));

    String test7 = "(121+(101+0))";
    double expected7 = (121+(101+0));

    String test8 = "(3*(5+2)*(10-7))";
    double expected8 = (3*(5+2)*(10-7));

    public void test() {
        DFSArithmeticEvaluator ae = new DFSArithmeticEvaluator();

        String test = test4;
        double expected = expected4;

        double val = ae.compute(test);
        System.out.println(val);
        System.out.println(expected);
        System.out.println(val == expected);
    }

    public static void main(String[] args) {
        ArithmeticEvaluatorTest tester = new ArithmeticEvaluatorTest();
        tester.test();
    }
}
