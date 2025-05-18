import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.time.*; // 时间 API
import java.time.format.DateTimeFormatter;
import java.util.HashMap; // 键值对集合 存日程用
import java.util.Map;
import cn.hutool.core.date.ChineseDate; // 第三方提供的农历转换工具

public class Calendar extends JFrame {
    private final JComboBox<Integer> ySelect; // 年份下拉框
    private final JComboBox<Integer> mSelect; // 月份下拉框
    private final JButton onMBt; // 上月按钮
    private final JButton nextMBt; // 下月按钮
    private final JTable jt; // 日历表格
    private final DefaultTableModel tm; // 表格模型
    private final JLabel DayTime; // 底部，当前选中日期+时间
    private final Timer timer; // 定时更新时间
    private final Map<LocalDate, String> sche = new HashMap<>(); // 日程集合

    public Calendar() {
        super("Java 日历");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(580, 413);
        setResizable(false); // 禁止调整窗口大小
        setLocationRelativeTo(null);

        // 顶部：年、月选择 + 前后按钮
        JPanel topPanel = new JPanel();
        ySelect = new JComboBox<>();
        mSelect = new JComboBox<>();
        onMBt = new JButton("上月");
        nextMBt = new JButton("下月");

        int yearNum = LocalDate.now().getYear();  // 当前日期LocalDate.now()
        // 填充年下拉框：1900~2099
        for (int y = yearNum - 125; y <= yearNum + 74; y++) {
            ySelect.addItem(y);
        }
        ySelect.setSelectedItem(yearNum); // 年份下拉框默认当前日期LocalDate.now()
        // 填充月下拉框
        for (int m = 1; m <= 12; m++) {
            mSelect.addItem(m);
        }
        mSelect.setSelectedItem(LocalDate.now().getMonthValue());  // 月份下拉框默认当前日期LocalDate.now()

        topPanel.add(ySelect);
        topPanel.add(new JLabel("年"));
        topPanel.add(mSelect);
        topPanel.add(new JLabel("月"));
        topPanel.add(onMBt);
        topPanel.add(nextMBt);
        add(topPanel, BorderLayout.NORTH);


        // 中部：日历表格
        String[] week = {"日", "一", "二", "三", "四", "五", "六"};
        tm = new DefaultTableModel(null, week);
        jt = new JTable(tm);
        jt.setRowHeight(50);
        jt.setDefaultRenderer(Object.class, new LunarRen()); // 设置表格所有单元格的默认渲染器为自定义的 LunarRen 类的实例，用于显示农历
        jt.setColumnSelectionAllowed(false); // 禁止整列选择
        jt.setRowSelectionAllowed(false); // 禁止整行选择
        jt.getTableHeader().setReorderingAllowed(false); // 禁止拖动列

        add(new JScrollPane(jt), BorderLayout.CENTER);

        // 底部：当前选中日期+时间
        DayTime = new JLabel();
        DayTime.setHorizontalAlignment(SwingConstants.CENTER);
        add(DayTime, BorderLayout.SOUTH);

        // 事件：重绘日历
        ActionListener refresh = e -> drawCal();
        ySelect.addActionListener(refresh);
        mSelect.addActionListener(refresh);
        onMBt.addActionListener(e -> {
            int m = mSelect.getItemAt(mSelect.getSelectedIndex());
            int y = ySelect.getItemAt(ySelect.getSelectedIndex());
            LocalDate d = LocalDate.of(y, m, 1).minusMonths(1);
            ySelect.setSelectedItem(d.getYear());
            mSelect.setSelectedItem(d.getMonthValue());
        });
        nextMBt.addActionListener(e -> {
            int m = mSelect.getItemAt(mSelect.getSelectedIndex());
            int y = ySelect.getItemAt(ySelect.getSelectedIndex());
            LocalDate d = LocalDate.of(y, m, 1).plusMonths(1); // 创建一个表示当前选定月份第一天的 LocalDate 对象，并将其减去一个月，得到上个月的日期
            ySelect.setSelectedItem(d.getYear());
            mSelect.setSelectedItem(d.getMonthValue());
        });

        // 点击某个单元格时，显示所选日期
        jt.addMouseListener(new MouseAdapter() {
            public void mouseClicked (MouseEvent e) {
                int row = jt.rowAtPoint(e.getPoint());
                int col = jt.columnAtPoint(e.getPoint());
                Object val = jt.getValueAt(row, col);
                if (val != null) {
                    int day = (Integer) val;
                    int y = (Integer) ySelect.getSelectedItem();
                    int m = (Integer) mSelect.getSelectedItem();
                    LocalDate sel = LocalDate.of(y, m, day);

                    // 弹出输入框，允许用户输入日程内容
                    String existing = sche.getOrDefault(sel, "");
                    String input = JOptionPane.showInputDialog(Calendar.this, "请输入日程内容：", existing);
                    if (input != null) {
                        if (input.trim().isEmpty()) {
                            sche.remove(sel); // 如果输入为空，移除日程
                        } else {
                            sche.put(sel, input); // 否则保存
                        }
                    }

                    // 更新底部信息栏
                    DayTime.setText("选中日期：" + sel.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + "  " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()));
                }
            }
        });

        // 定时器更新时间（让底部时分秒动起来）
        timer = new Timer(1000, e -> {
            String text = DayTime.getText();
            if (text.startsWith("选中日期")) {
                // 保持选中日期前半段，只更新时分秒
                String front = text.split("  ")[0];
                DayTime.setText(front + "  " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()));
            } else {
                // 获取当前时间
                LocalDate now = LocalDate.now();
                DayTime.setText(
                        "当前日期：" + now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) +
                                "  " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now())
                );
            }
        });
        timer.start();
        drawCal();  // 首次绘制
        setVisible(true);
    }

    private void drawCal() {
        // 清空表格
        tm.setRowCount(0);

        int year = (Integer) ySelect.getSelectedItem();
        int month = (Integer) mSelect.getSelectedItem();
        LocalDate first = LocalDate.of(year, month, 1); // 创建一个 LocalDate 对象，表示指定年月的 1 号
        int firstDay = first.getDayOfWeek().getValue() % 7;
        int mLength = first.lengthOfMonth();

        // 6 行 7 列
        Object[][] cell = new Object[6][7];
        int day = 1;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 && c < firstDay) {
                    cell[r][c] = null;
                } else if (day > mLength) {
                    cell[r][c] = null;
                } else {
                    cell[r][c] = day++;
                }
            }
        }
        // 填入模型
        for (Object[] row : cell) {
            tm.addRow(row);
        }
    }

    // 自定义农历单元格渲染
    class LunarRen extends JLabel implements TableCellRenderer {
        public LunarRen() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.TOP);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value != null) {
                int day = (Integer) value;
                int year = (Integer) ySelect.getSelectedItem();
                int month = (Integer) mSelect.getSelectedItem();
                    // 公历转农历
                    ChineseDate lunar = new ChineseDate(LocalDate.of(year, month, day));
                    String lunarText = lunar.getChineseMonth() + lunar.getChineseDay();

                    // 设置单元格文本
                    setText("<html><center>" + day + "<br>" +
                            "<font size=-2>" + lunarText + "</font></center></html>");
            } else {
                setText("");
            }

            // 样式设置
            setBackground(UIManager.getColor("Table.background"));
            return this;
        }
    }
    public static void main(String[] args) {
        // 使用系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        new Calendar();
    }
}
