import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class ScientificCalculator extends JFrame implements ActionListener {

    /* ---------- UI ---------- */
    private final JLabel  history;
    private final JTextField display;

    /* ---------- State ---------- */
    private String currentInput = "";
    private String operator     = "";
    private String expression   = "";
    private double result       = 0;
    private boolean resultShown = false;

    /* ---------- Layout ---------- */
    private final String[][] layout = {
            {"sin","cos","tan","asin","atan"},
            {"cot","π","e","C","Del"},           // ⬅️ 2nd → cot, ⌫ → Del
            {"x²","1/x","|x|","exp","mod"},
            {"√","(",")","n!","÷"},
            {"xʸ","7","8","9","×"},
            {"10ˣ","4","5","6","−"},
            {"log","1","2","3","+"},
            {"ln","±","0",".","="}
    };
    private final Map<String,JButton> buttons = new HashMap<>();

    /* ---------- Constructor ---------- */
    public ScientificCalculator() {
        super("Scientific Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        /* ----- Color palette ----- */
        Color bgStart    = new Color(40, 40, 50);
        Color bgEnd      = new Color(25, 45, 95);
        Color bgButton   = new Color(60, 63, 80);
        Color bgButtonEq = new Color(108, 99, 255);
        Color fgText     = Color.WHITE;
        Color borderClr  = new Color(90, 90, 120);

        /* ----- Gradient panel ----- */
        GradientPanel top = new GradientPanel(bgStart, bgEnd);
        top.setLayout(new BorderLayout(5,5));
        add(top, BorderLayout.CENTER);

        history = new JLabel(" ", SwingConstants.RIGHT);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        history.setForeground(new Color(190,190,210));
        history.setBorder(BorderFactory.createEmptyBorder(6,10,0,10));
        top.add(history, BorderLayout.NORTH);

        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(new Font("Consolas", Font.BOLD, 34));
        display.setOpaque(false);
        display.setForeground(fgText);
        display.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
        top.add(display, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(layout.length, layout[0].length,4,4));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(5,5,10,5));

        for (String[] row : layout) {
            for (String label : row) {
                JButton btn = new JButton(label);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                btn.setForeground(fgText);
                btn.setBackground(label.equals("=") ? bgButtonEq : bgButton);
                btn.setBorder(new LineBorder(borderClr));
                btn.setFocusPainted(false);
                btn.setPreferredSize(new Dimension(60,50));   // width, height
                btn.addActionListener(this);
                grid.add(btn);
                buttons.put(label, btn);
            }
        }
        top.add(grid, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* ---------- Event handling ---------- */
    @Override public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            switch (cmd) {
                /* ---- Clear / Backspace ---- */
                case "C":  reset(); break;
                case "Del":
                    if (!currentInput.isEmpty()) {
                        currentInput = currentInput.substring(0, currentInput.length()-1);
                        updateDisplay(currentInput.isEmpty() ? "0" : currentInput);
                    }
                    break;

                /* ---- Unary ops ---- */
                case "sin":  unary(Math::sin,  "sin");  break;
                case "cos":  unary(Math::cos,  "cos");  break;
                case "tan":  unary(Math::tan,  "tan");  break;
                case "cot":  unary(x -> 1/Math.tan(x), "cot"); break;
                case "asin": unary(Math::asin, "asin"); break;
                case "atan": unary(Math::atan, "atan"); break;

                case "x²":  unary(x -> Math.pow(x,2), "sqr"); break;
                case "√":   unary(Math::sqrt,         "√");   break;
                case "1/x": unary(x -> 1/x,           "1/x"); break;
                case "|x|": unary(Math::abs,          "|x|"); break;
                case "exp": unary(Math::exp,          "exp"); break;
                case "10ˣ": unary(x -> Math.pow(10,x),"10^x");break;
                case "n!":  unary(this::factorial,    "n!");  break;
                case "log": unary(Math::log10,        "log"); break;
                case "ln":  unary(Math::log,          "ln");  break;

                /* ---- Constants / sign ---- */
                case "π": putConstant(Math.PI,"π"); break;
                case "e": putConstant(Math.E,"e");  break;
                case "±": toggleSign();            break;

                /* ---- Binary ops ---- */
                case "+": case "−": case "×": case "÷": case "mod": case "xʸ":
                    storeOperator(cmd); break;

                /* ---- Evaluate ---- */
                case "=": evaluate(); break;

                /* ---- Digits & misc ---- */
                case ".", "0","1","2","3","4","5","6","7","8","9","(",")":
                    append(cmd); break;
            }
        } catch(Exception ex) {
            history.setText(" ");
            updateDisplay("Error");
            resetInternals();
        }
    }

    /* ---------- Core Math ---------- */
    private void unary(DoubleUnaryOperator op, String name) {
        if (currentInput.isEmpty() && resultShown) currentInput = display.getText();
        if (!currentInput.isEmpty()) {
            double v = Double.parseDouble(currentInput);
            double r = op.applyAsDouble(v);
            history.setText(name + "(" + trim(v) + ") =");
            updateDisplay(trim(r));
            currentInput = String.valueOf(r);
            resultShown = true;
        }
    }

    private void storeOperator(String op) {
        double first = currentInput.isEmpty()
                ? Double.parseDouble(display.getText())
                : Double.parseDouble(currentInput);
        result     = first;
        operator   = op;
        expression = trim(first) + " " + op + " ";
        history.setText(expression);
        currentInput = "";
        resultShown = false;
    }

    private void evaluate() {
        if (operator.isEmpty() || currentInput.isEmpty()) return;
        double operand = Double.parseDouble(currentInput);

        switch (operator) {
            case "+":   result += operand;            break;
            case "−":   result -= operand;            break;
            case "×":   result *= operand;            break;
            case "÷":   result /= operand;            break;
            case "mod": result %= operand;            break;
            case "xʸ":  result  = Math.pow(result, operand); break;
        }
        history.setText(expression + trim(operand) + " =");
        updateDisplay(trim(result));

        currentInput = "";
        operator     = "";
        resultShown  = true;
    }

    /* ---------- Helpers ---------- */
    private void append(String s) {
        if (resultShown) {
            currentInput = "";
            history.setText(" ");
            resultShown = false;
        }
        currentInput += s;
        updateDisplay(currentInput);
    }

    private void putConstant(double c, String label) {
        if (resultShown) { history.setText(" "); resultShown = false; }
        currentInput = String.valueOf(c);
        updateDisplay(trim(c));
        history.setText(label);
    }

    private void toggleSign() {
        if (currentInput.isEmpty()) return;
        currentInput = currentInput.startsWith("-")
                ? currentInput.substring(1)
                : "-" + currentInput;
        updateDisplay(currentInput);
    }

    private double factorial(double n) {
        if (n < 0 || n != Math.floor(n)) return Double.NaN;
        double f = 1;
        for (int i = 2; i <= (int)n; i++) f *= i;
        return f;
    }

    private String trim(double d) {
        return (d == (long)d) ? String.valueOf((long)d) : String.valueOf(d);
    }

    private void updateDisplay(String t) { display.setText(t); }

    private void reset() {
        resetInternals();
        history.setText(" ");
        updateDisplay("0");
    }

    private void resetInternals() {
        currentInput = "";
        operator     = "";
        expression   = "";
        result       = 0;
        resultShown  = false;
    }

    /* ---------- Gradient panel ---------- */
    private static class GradientPanel extends JPanel {
        private final Color start, end;
        GradientPanel(Color start, Color end) {
            this.start = start; this.end = end;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0,0,start,getWidth(),getHeight(),end));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.dispose();
        }
    }

    /* ---------- main ---------- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScientificCalculator::new);
    }
}
