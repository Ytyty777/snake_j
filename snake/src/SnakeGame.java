import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SnakeGame extends JFrame {
    public SnakeGame() {
    setTitle("Snake");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    GamePanel gamePanel = new GamePanel();
    add(gamePanel);
    pack(); // <-- automatyczny rozmiar ramki
    setVisible(true);
}
    public static void main(String[] args) {
        new SnakeGame();
    }
}



class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final List<Point> obstacles = new ArrayList<>();
    final Object lock = new Object();
    private int movingFoodMoveCounter = 0;
    private final int MOVING_FOOD_INTERVAL = 10;
    private boolean isAliveAI1 = true;
    private boolean isAliveAI2 = true;
    private volatile boolean game_over = true;
    private String message = "";
    private boolean inMenu = true;
    public volatile int que = 0;
    private List<Integer> bestScores = new ArrayList<>();

    private int playerScore = 0;
    private int ai1Score = 0;
    private int ai2Score = 0;
    private String status = "wygrana";

    private final int TILE_SIZE = 20;
    private final int WIDTH = 800 / TILE_SIZE;
    private final int HEIGHT = 600 / TILE_SIZE;

    private final List<Point> snake = new ArrayList<>();
    private final List<Point> snakeAI1 = new ArrayList<>();
    private final List<Point> snakeAI2 = new ArrayList<>();

    private Point food;
    private char direction = 'R';
    private char directionAI1 = 'R';
    private char directionAI2 = 'R';

    private final Timer timer;
    private Point movingFood;
    private boolean hasMovingFood = false;

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadBestScores();

        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        


        // Inicjalizujemy węże
        snake.add(new Point(5, 5));
        snakeAI1.add(new Point(15, 15));
        snakeAI2.add(new Point(25, 25));

        obstacles.clear();
         Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            obstacles.add(new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT)));
        }

        //spawnFood();
        food = null;

        timer = new Timer(100, this);
        timer.start();

        startAIThreads();

        new Thread(() -> {
            //Random rand = new Random();
            while (true) {
                try {
                    Thread.sleep(3000); // Co 3s sprawdzamy
                } catch (InterruptedException ignored) {}

                if (food == null) {
                    createFood();
                }
            }
        }).start();

    }

    private void loadBestScores() {
    bestScores.clear();
    try (Scanner sc = new Scanner(new File("wyniki.txt"))) {
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            // Szukamy "Gracz: X"
            if (line.contains("Gracz:")) {
                String[] parts = line.split(",");
                String playerPart = parts[0]; // "Gracz: X"
                int score = Integer.parseInt(playerPart.replaceAll("\\D", ""));
                bestScores.add(score);
            }
        }
    } catch (Exception ignored) {
    }
    bestScores.sort(Collections.reverseOrder());
    if (bestScores.size() > 5) {
        bestScores = bestScores.subList(0, 5);
    }
}

    private Point nextPoint(Point head, char dir) {
    Point p = new Point(head);
    switch (dir) {
        case 'U': p.y--; break;
        case 'D': p.y++; break;
        case 'L': p.x--; break;
        case 'R': p.x++; break;
    }
    return p;
    }

    private void saveScores() {
        try (FileWriter fw = new FileWriter("wyniki.txt", true)) {
            fw.write("Status:"+ status+ "Gracz: " + playerScore + ", AI1: " + ai1Score + ", AI2: " + ai2Score + '\n');
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    private void createFood() {
    Random rand = new Random();
    food = new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT));
}


    private void spawnMovingFood() {
    Random rand = new Random();
    movingFood = new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT));
    hasMovingFood = true;
    }

    private void moveMovingFood() {
    if (!hasMovingFood) return;

    Random rand = new Random();
    Point[] directions = {
        new Point(0, -1), // góra
        new Point(0, 1),  // dół
        new Point(-1, 0), // lewo
        new Point(1, 0)   // prawo
    };
    Point dir = directions[rand.nextInt(directions.length)];
    Point newPoint = new Point(movingFood.x + dir.x, movingFood.y + dir.y);

    // Przemieszczamy, gdy nowa pozycja jest wewnątrz mapy
    if (newPoint.x >= 0 && newPoint.y >= 0 && newPoint.x < WIDTH && newPoint.y < HEIGHT) {
        movingFood = newPoint;
    }
}

    private void runAI(List<Point> snakeAI, boolean isFirstAI) {
    while (true) {
        //synchronized (lock){
            /*if(isFirstAI){
                while (que != 1) {
                try { lock.wait(); } catch (InterruptedException e) {}
                } 
            }
            if(!isFirstAI){
                while (que != 2 ) {
                try { lock.wait(); } catch (InterruptedException e) {}
                }
            }*/

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {} 
            

            Point head = snakeAI.get(0);
            List<Character> possibleDirs = new ArrayList<>(List.of('U','D','L','R'));

            // Odrzucamy kierunek odwrotny
            if (isFirstAI) {
                if (directionAI1 == 'U') possibleDirs.remove((Character) 'D');
                if (directionAI1 == 'D') possibleDirs.remove((Character) 'U');
                if (directionAI1 == 'L') possibleDirs.remove((Character) 'R');
                if (directionAI1 == 'R') possibleDirs.remove((Character) 'L');
            } else {
                if (directionAI2 == 'U') possibleDirs.remove((Character) 'D');
                if (directionAI2 == 'D') possibleDirs.remove((Character) 'U');
                if (directionAI2 == 'L') possibleDirs.remove((Character) 'R');
                if (directionAI2 == 'R') possibleDirs.remove((Character) 'L');
            }

            // Logika ustawiania nowego kierunku
            if (food != null) {
                possibleDirs.sort((dir1, dir2) -> {
                Point next1 = nextPoint(head, dir1);
                Point next2 = nextPoint(head, dir2);
                int distance1 = Math.abs(next1.x - food.x) + Math.abs(next1.y - food.y);
                int distance2 = Math.abs(next2.x - food.x) + Math.abs(next2.y - food.y);
                return Integer.compare(distance1, distance2);
            });
            }

            for (char candidate : possibleDirs) {
                Point nextHead = nextPoint(head, candidate);
                if (!isPointCollision(nextHead, snakeAI, snake, snakeAI1, snakeAI2) && !obstacles.contains(nextHead)) {
                    if (isFirstAI) {
                        directionAI1 = candidate;
                    } else {
                        directionAI2 = candidate;
                    }
                    break;
                }
            }

            
                /* 
                if (isFirstAI) {
                    if(!game_over){
                    moveSnake(snakeAI, directionAI1);
                    //que=2;
                    if (isCollision(snakeAI) || isCollisionWithOther(snakeAI, snake) || isCollisionWithOther(snakeAI, snakeAI2) || isCollisionWithObstacles(snakeAI)) {
                        message = "Wygrałeś"; 
                        status = message;
                        game_over=true;
                        saveScores();
                        repaint(); 
                        resetGame();
                        continue;
                      }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {} 
                    que = 2;
                    lock.notifyAll();
                    
                 } else {
                    if(!game_over){
                        moveSnake(snakeAI, directionAI2);
                        //que=0;
                        if (isCollision(snakeAI) || isCollisionWithOther(snakeAI, snake) || isCollisionWithOther(snakeAI, snakeAI1) || isCollisionWithObstacles(snakeAI)) {
                            message = "Wygrałeś"; 
                            status = message;
                            game_over=true;
                            saveScores();
                            repaint(); 
                            resetGame();
                            continue;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {} 
                    que = 0;
                    lock.notifyAll();
                    
                 }*/
                }  
            //} 
    }
         

    private boolean isPointCollision(Point p, List<Point>... snakes) {
        if (p.x < 0 || p.y < 0 || p.x >= WIDTH || p.y >= HEIGHT) {
            return true;
        }
        for (List<Point> s : snakes) {
            if (s.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private void startAIThreads() {
        Thread t1 = new Thread(() -> runAI(snakeAI1, true));
        Thread t2 = new Thread(() -> runAI(snakeAI2, false));
        t1.start();
        t2.start();
    }

    private void moveSnake(List<Point> s, char dir) {
        Point head = new Point(s.get(0));
        switch (dir) {
            case 'U': head.y--; break;
            case 'D': head.y++; break;
            case 'L': head.x--; break;
            case 'R': head.x++; break;
        }
        s.add(0, head);
    }

    private boolean isCollision(List<Point> s) {
        Point head = s.get(0);
        return (head.x < 0 || head.y < 0 || head.x >= WIDTH || head.y >= HEIGHT || s.subList(1, s.size()).contains(head));
    }

    private boolean isCollisionWithOther(List<Point> snake, List<Point> other) {
    Point head = snake.get(0);
    // Kolizja z jakąkolwiek częścią
    if (other.contains(head)) {
        return true;
    }
    // Dodatkowe sprawdzenie: czołowe zderzenie (head vs head)
    if (other.get(0).equals(head)) {
        return true;
    }
    return false;
}


    private void resetGame() {
        snake.clear();
        snakeAI1.clear();
        snakeAI2.clear();

        snake.add(new Point(5, 5));
        snakeAI1.add(new Point(15, 15));
        snakeAI2.add(new Point(25, 25));

        direction = 'R';
        directionAI1 ='R';
        directionAI2 = 'R';
        //spawnFood();
        createFood();
        playerScore = 0;
        ai1Score = 0;
        ai2Score = 0;
        
    }

    private boolean consumeIfNeeded(List<Point> s) {
    if (s.get(0).equals(food)) {
        //spawnFood();
        food = null;
        createFood();
        return true;
    } else {
        s.remove(s.size() - 1);
        return false;
    }
}


    private boolean isCollisionWithObstacles(List<Point> s) {
    Point head = s.get(0);
    for (Point o : obstacles) {
        if (head.equals(o)) {
            return true;
        }
    }
    return false;
}
    

    @Override
    public void actionPerformed(ActionEvent e) {
        if (game_over){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            game_over=false;
            message="";
            //resetGame();
            /*t1.join();
            t2.join();
            startAIThreads();*/
        }
        //synchronized (lock) {
            //if(que!=0){
                if (!game_over){
                // Ruch gracza
                moveSnake(snake, direction);
                //que=1;
                if (isCollision(snake) || isCollisionWithOther(snake, snakeAI1) || isCollisionWithOther(snake, snakeAI2) || isCollisionWithObstacles(snake)) {
                    message = "Przegrałeś ";
                    status=message;
                    repaint(); 
                    game_over=true;
                    saveScores();
                    resetGame();
                    return;
                }
            //}
            /*que=1;
            lock.notifyAll();*/
        //}
        
            
            // Ruch AI1
            if (isAliveAI1) {
                moveSnake(snakeAI1, directionAI1);
                if (isCollision(snakeAI1) || isCollisionWithOther(snakeAI1, snake) || isCollisionWithOther(snakeAI1, snakeAI2) || isCollisionWithObstacles(snakeAI1)) {
                    message = "Wygrałeś "; 
                    status=message;
                    repaint();
                    game_over=true;
                    saveScores();
                    resetGame();
                    return;
                }
            }


            // Ruch AI2
            // Ruch AI2
            if (isAliveAI2) {
                moveSnake(snakeAI2, directionAI2);
                if (isCollision(snakeAI2) || isCollisionWithOther(snakeAI2, snake) || isCollisionWithOther(snakeAI2, snakeAI1) || isCollisionWithObstacles(snakeAI2)) {
                    message = "Wygrałeś "; 
                    status=message;
                    repaint();
                    game_over=true;
                    saveScores();
                    resetGame();
                    return;
                }
            }


            // Losowo generujemy nowy typ owocu
            if (!hasMovingFood && new Random().nextInt(100) < 2) {
                spawnMovingFood();
            }

            // Przemieszczamy „żywy” owoc
            // Przemieszczamy „żywy” owoc rzadziej
            if (hasMovingFood) {
                movingFoodMoveCounter++;
                if (movingFoodMoveCounter >= MOVING_FOOD_INTERVAL) {
                    moveMovingFood();
                    movingFoodMoveCounter = 0;
                }
            }

            // Sprawdzamy zjedzenie „żywego” owocu
            // Sprawdzamy zjedzenie „żywego” owocu
            if (hasMovingFood) {
                if (snake.get(0).equals(movingFood)) {
                    hasMovingFood = false;
                    snake.add(new Point(snake.get(snake.size() - 1))); // Powiększamy gracza
                    playerScore+=10;
                } else if (snakeAI1.get(0).equals(movingFood)) {
                    hasMovingFood = false;
                    snakeAI1.add(new Point(snakeAI1.get(snakeAI1.size() - 1)));
                    ai1Score+=10;
                } else if (snakeAI2.get(0).equals(movingFood)) {
                    hasMovingFood = false;
                    snakeAI2.add(new Point(snakeAI2.get(snakeAI2.size() - 1)));
                    ai2Score+=10;
                }
            }

            Point nextHeadAI1 = nextPoint(snakeAI1.get(0), directionAI1);
            Point nextHeadAI2 = nextPoint(snakeAI2.get(0), directionAI2);

            if (nextHeadAI1.equals(nextHeadAI2)) {
                // Kolizja przewidywana: obie głowy zmierzają na to samo pole
                // Rozwiązanie przykładowe: zmieniamy kierunek AI2 na inny (np. próbujemy 'L')
                if (directionAI2 != 'L') {
                    directionAI2 ='L'; 
                } else {
                    directionAI2 ='R'; 
                }
            }

            if (consumeIfNeeded(snake)) {
                playerScore++;
            }
            if (isAliveAI1 && consumeIfNeeded(snakeAI1)) {
                ai1Score++;
            }
            if (isAliveAI2 && consumeIfNeeded(snakeAI2)) {
                ai2Score++;
            }


            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (inMenu) {
            g.setColor(Color.WHITE);
            g.drawString("1. Zacznij grę", 300, 250);
            g.drawString("Najlepsze wyniki:", 300, 300);
            for (int i = 0; i < bestScores.size() && i < 5; i++) {
                g.drawString((i + 1) + ". " + bestScores.get(i), 300, 325 + i * 20);
            }
        } else {
         
        

        // Jedzenie
        g.setColor(Color.RED);
        g.fillOval(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Gracz
        g.setColor(Color.GREEN);
        for (Point p : snake) {
            g.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // AI1
        g.setColor(Color.BLUE);
        for (Point p : snakeAI1) {
            g.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // AI2
        g.setColor(Color.YELLOW);
        for (Point p : snakeAI2) {
            g.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // Żywy owoc
        if (hasMovingFood) {
            g.setColor(Color.MAGENTA);
            g.fillOval(movingFood.x * TILE_SIZE, movingFood.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        if (!message.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(30f));
            g.drawString(message, getWidth() / 2 - 100, getHeight() / 2);
}

        g.setColor(Color.GRAY);
        for (Point o : obstacles) {
            g.fillRect(o.x * TILE_SIZE, o.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.WHITE);
        g.drawString("Gracz: " + playerScore, 10, 20);
        g.drawString("AI1: " + ai1Score, 10, 40);
        g.drawString("AI2: " + ai2Score, 10, 60);
        }


    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (inMenu && e.getKeyCode() == KeyEvent.VK_1) {
            inMenu = false;
            resetGame();
        } else if (!inMenu) {
        char newDir = direction;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: newDir = 'U'; break;
            case KeyEvent.VK_DOWN: newDir = 'D'; break;
            case KeyEvent.VK_LEFT: newDir = 'L'; break;
            case KeyEvent.VK_RIGHT: newDir = 'R'; break;
        }
        if ((direction == 'U' && newDir != 'D') ||
            (direction == 'D' && newDir != 'U') ||
            (direction == 'L' && newDir != 'R') ||
            (direction == 'R' && newDir != 'L')) {
            direction = newDir;
        }
    }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
