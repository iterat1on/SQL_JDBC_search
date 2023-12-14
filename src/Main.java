import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/univ";
    private static final String USER = "root";
    private static final String PASS = "alpine";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int menu;

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            do {
                System.out.println("메뉴를 선택하세요.");
                System.out.println("1. 융합과목 조회");
                System.out.println("2. 수업 시간 및 강의실 조회");
                System.out.println("3. 프로젝트 수행결과 조회");
                System.out.println("4. 종료");
                menu = scanner.nextInt();

                switch (menu) {
                    case 1:
                        getMergedCourses(conn);
                        break;
                    case 2:
                        getClassTimeAndClassroom(conn, scanner);
                        break;
                    case 3:
                        getStudentProjectResults(conn, scanner);
                        break;
                    case 4:
                        System.out.println("프로그램을 종료합니다.");
                        break;
                    default:
                        System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
                        break;
                }
            } while (menu != 4);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void getMergedCourses(Connection conn) {
        String query = "SELECT c.course_id, c.title, c.credits, GROUP_CONCAT(DISTINCT c2.dept_name ORDER BY c2.dept_name SEPARATOR ', ') AS dept_names " +
                "FROM course c " +
                "INNER JOIN course_2 c2 ON c.course_id = c2.course_id " +
                "WHERE c.course_id IN (" +
                "SELECT course_id FROM course_2 GROUP BY course_id HAVING COUNT(course_id) > 1) " +
                "GROUP BY c.course_id, c.title, c.credits";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println();
                System.out.println("과목번호: " + rs.getString("course_id"));
                System.out.println("과목명: " + rs.getString("title"));
                System.out.println("학점: " + rs.getInt("credits"));
                System.out.println("개설학과: " + rs.getString("dept_names"));
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void getClassTimeAndClassroom(Connection conn, Scanner scanner) {
        System.out.print("연도를 입력하세요: ");
        int year = scanner.nextInt();
        System.out.print("학기를 입력하세요: ");
        String semester = scanner.next();
        System.out.println();

        String query = "SELECT course_id, title, sec_id, day, start_time, end_time, building, room_number " +
                "FROM section NATURAL JOIN course JOIN class_slot ON section.class_slot_id = class_slot.class_slot_id JOIN time_slot ON class_slot.time_slot_id = time_slot.time_slot_id " +
                "WHERE year = ? AND semester = ? " +
                "ORDER BY course_id, sec_id, day, start_time";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, year);
            pstmt.setString(2, semester);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("과목번호: " + rs.getString("course_id"));
                System.out.println("과목명: " + rs.getString("title"));
                System.out.println("분반번호: " + rs.getString("sec_id"));
                System.out.println("요일: " + rs.getString("day"));
                System.out.println("시작시간: " + rs.getString("start_time"));
                System.out.println("종료시간: " + rs.getString("end_time"));
                System.out.println("강의실 빌딩: " + rs.getString("building"));
                System.out.println("강의실 호실: " + rs.getString("room_number"));
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void getStudentProjectResults(Connection conn, Scanner scanner) {
        System.out.print("연도를 입력하세요: ");
        int year = scanner.nextInt();
        System.out.print("학기를 입력하세요: ");
        String semester = scanner.next();
        System.out.print("학생의 학번을 입력하세요: ");
        System.out.println();
        int studentId = scanner.nextInt();

        String query1 = "SELECT ID, name, dept_name " +
                "FROM student " +
                "WHERE ID = ?";

        String query2 = "SELECT course_id, title, sec_id, grade " +
                "FROM takes NATURAL JOIN course " +
                "WHERE ID = ? AND year = ? AND semester = ?";

        String query3 = "SELECT course_id, project_id, S_id, I_id, score " +
                "FROM project " +
                "WHERE S_id = ? AND year = ? AND semester = ? AND course_id = ?";


        String query4 = "SELECT name " +
                "FROM instructor " +
                "WHERE ID = ?";

        try (PreparedStatement pstmt1 = conn.prepareStatement(query1);
             PreparedStatement pstmt2 = conn.prepareStatement(query2);
             PreparedStatement pstmt3 = conn.prepareStatement(query3);
             PreparedStatement pstmt4 = conn.prepareStatement(query4)) {

            pstmt1.setInt(1, studentId);

            ResultSet rs1 = pstmt1.executeQuery();
            if (rs1.next()) {
                System.out.println("학번: " + rs1.getInt("ID"));
                System.out.println("이름: " + rs1.getString("name"));
                System.out.println("학과: " + rs1.getString("dept_name"));
                System.out.println();

                pstmt2.setInt(1, studentId);
                pstmt2.setInt(2, year);
                pstmt2.setString(3, semester);

                ResultSet rs2 = pstmt2.executeQuery();
                HashMap<String, ArrayList<Integer>> courseScores = new HashMap<>();

                while (rs2.next()) {
                    String courseId = rs2.getString("course_id");
                    System.out.println("과목번호: " + courseId);
                    System.out.println("과목명: " + rs2.getString("title"));
                    System.out.println("분반번호: " + rs2.getString("sec_id"));
                    System.out.println("성적: " + rs2.getString("grade"));
                    System.out.println();

                    pstmt3.setInt(1, studentId);
                    pstmt3.setInt(2, year);
                    pstmt3.setString(3, semester);
                    pstmt3.setString(4, courseId);

                    ResultSet rs3 = pstmt3.executeQuery();
                    while (rs3.next()) {
                        int score = rs3.getInt("score");

                        if (!courseScores.containsKey(courseId)) {
                            courseScores.put(courseId, new ArrayList<Integer>());
                        }
                        courseScores.get(courseId).add(score);

                        String instructorId = rs3.getString("I_id");
                        pstmt4.setInt(1, Integer.parseInt(instructorId));
                        ResultSet rs4 = pstmt4.executeQuery();
                        if (rs4.next()) {
                            System.out.println("학생이름: " + rs1.getString("name"));
                            System.out.println("점수: " + score);
                            System.out.println("지도교수 이름: " + rs4.getString("name"));
                            System.out.println();
                        }
                    }

                    if (courseScores.containsKey(courseId)) {
                        ArrayList<Integer> scores = courseScores.get(courseId);
                        double average = scores.stream().mapToInt(Integer::intValue).average().getAsDouble();
                        System.out.println("프로젝트 횟수: " + scores.size());
                        System.out.println("프로젝트 점수 평균: " + average);
                        System.out.println();
                    }
                }

            } else {
                System.out.println("해당 학생을 찾을 수 없습니다.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}


