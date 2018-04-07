import java.io.*;
import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

class Window extends WindowAdapter implements ActionListener {
    Frame f;
    MenuBar mb;
    Menu m_menu;
    MenuItem m_login, m_exit;
    TextArea ta;
    Dialog a;
    Label u, p, d;
    TextField name_in, passwd_in, date_in;
    Button submit, cancel;
    Date date = null;
    SimpleDateFormat sdf;

    public Window(SimpleDateFormat sdf, Date date) {
        f = new Frame();
        f.setSize(400, 300);
        f.setLocation(300, 300);
        mb = new MenuBar();
        f.setMenuBar(mb);
        ta = new TextArea("");
        ta.setEditable(false);
        ta.setFocusable(false);
        f.add(ta);
        m_menu = new Menu("menu");
        m_login = new MenuItem("login");
        m_exit = new MenuItem("exit");
        m_menu.add(m_login);
        m_menu.add(m_exit);
        mb.add(m_menu);
        m_login.addActionListener(this);
        m_exit.addActionListener(this);
        a = new Dialog(f, "login");
        a.setLayout(new FlowLayout());
        a.setSize(350, 300);
        u = new Label("user name:");
        p = new Label("user password");
        name_in = new TextField("");
        passwd_in = new TextField("");
        d = new Label("login date");

        date_in = new TextField(sdf.format(date));
        this.sdf = sdf;
        a.add(u);
        a.add(name_in);
        a.add(p);
        a.add(passwd_in);
        a.add(d);
        a.add(date_in);
        submit = new Button("submit");
        cancel = new Button("cancel");
        submit.addActionListener(this);
        cancel.addActionListener(this);
        a.add(submit);
        a.add(cancel);
        a.addWindowListener(this);
        f.setVisible(true);
        f.addWindowListener(this);
    }

    public void windowClosing(WindowEvent e) {
        if (e.getWindow() == a) {
            a.setVisible(false);
            ta.setText("You cancel the login!");
            return;
        }
        System.exit(0);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == m_login) {
            a.setVisible(true);
        }
        if (e.getSource() == m_exit) {
            System.exit(0);
        }
        if (e.getSource() == submit) {
            String name = name_in.getText();
            String password = passwd_in.getText();
            Connection con;
            Statement sql;
            ResultSet rs;
            int userFlag = 0;//用户存在状态
            int passwdFlag = 0;//密码正确状态
            int lockFlag = 0;//账户锁定状态
            int fault_time = 0;//登录错误次数统计
            int fault_time_num = 0;//错误登录次数参数表阈值
            int days = 0;//最近登录日期限制
            Date date_server = null;
            try {
                this.date = this.sdf.parse(date_in.getText());
            } catch (Exception edate) {
                System.out.println(edate.toString());
            }
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                /*take your own database*/
                con = DriverManager
                        .getConnection("jdbc:sqlserver://127.0.0.1:1433/*xxx*/;DataBaseName=/*xxx*/;user=/*xxx*/;password=/*xxx*/");

                //判断用户是否存在
                sql = con.createStatement();
                rs = sql.executeQuery("select username from Users;");

                while (rs.next()) {
                    if (rs.getString("username").equals(name)) {
                        userFlag = 1;
                    } else {
                        ta.setText("用户名或密码错误！");
                        con.close();
                        a.setVisible(false);
                        return;
                    }
                }
                
                //判断密码是否正确
                sql = con.createStatement();
                rs = sql.executeQuery(
                        "select password, fault_time, num, days from Users, Argments where Users.username = " + name);
                while (rs.next()) {
                    fault_time = rs.getInt(2);
                    fault_time_num = rs.getInt(3);
                    days = rs.getInt(4);
                    if (password.equals(rs.getString(1))) {
                        passwdFlag = 1;
                    } else {
                        fault_time++;
                        ta.setText("用户名或密码错误！");
                        if (fault_time > fault_time_num) {
                            //设置lock
                        }
                        con.close();
                        return;
                    }
                }

                //用户账户是否处于锁定状态
                sql = con.createStatement();
                if (userFlag == 1 && passwdFlag == 1
                        && (rs = sql.executeQuery("select unlock_time from Users where username = " + name)).next()) {
                    Date unlock_time_now = rs.getTimestamp(1);
                    date_server = new Date();
                    sql = con.createStatement();
                    rs = sql.executeQuery("select getDate()");
                    while (rs.next()) {
                        date_server = rs.getTimestamp(1);
                    }
                    if (unlock_time_now != null && unlock_time_now.after(date_server)) {
                        ta.setText("正在锁定状态！\n" + "将于 " + unlock_time_now.toString() + " 后解锁");
                        a.setVisible(false);
                        con.close();
                        return;
                    }
                }

                //登录日期控制
                long difference = date_server.getTime() - this.date.getTime();
                if (difference < 0 || difference > fault_time_num * 24 * 3600 * 1000) {
                    ta.setText("只能登录最近 " + days + " 天的日期！");      
                    a.setVisible(false);
                    con.close();
                    return;
                }

                //登录成功
                if (userFlag == 1 && passwdFlag == 1 && lockFlag == 0) {
                    sql = con.createStatement();
                    rs = sql.executeQuery("select * from Users where username=" + name);
                    while (rs.next()) {
                        ta.setText("登录成功！");
                        //con.close();
                        a.setVisible(false);
                    }
                }
                

            } catch (Exception e1) {
                ta.setText("error: " + e1.toString());
            }
        }
        if (e.getSource() == cancel) {
            a.setVisible(false);
            ta.setText("You cancel the login!");
        }

    }
}

public class login {
    public static void main(String[] args) {
        Date date = new Date();
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = sdFormat.parse(sdFormat.format(date));
        } catch (Exception ee) {
            System.out.println(ee.toString());
        }
        Window w = new Window(sdFormat, date);
    }
}
