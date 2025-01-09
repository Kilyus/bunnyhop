package com.example.bunnyhop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class BunnyHopPlugin extends JavaPlugin implements Listener {

    // Lưu trữ thời gian, tốc độ và góc yaw của người chơi
    private final HashMap<UUID, Long> groundTouchTime = new HashMap<>();
    private final HashMap<UUID, Double> speedFactors = new HashMap<>();
    private final HashMap<UUID, Float> lastYaw = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BunnyHopPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BunnyHopPlugin has been disabled!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Kiểm tra khi người chơi tiếp đất và đã quá 0.2 giây
        if (player.isOnGround()) {
            handleGroundTouch(player, playerId);
            return;
        }

        // Kiểm tra người chơi có cầm vật phẩm "Đất"
        if (player.getInventory().getItemInMainHand().getType() != Material.DIRT) {
            return;
        }

        // Xử lý vận tốc khi người chơi di chuyển
        handlePlayerSpeed(player, playerId);
    }

    private void handleGroundTouch(Player player, UUID playerId) {
        groundTouchTime.put(playerId, System.currentTimeMillis());

        // Reset vận tốc khi người chơi chạm đất
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - groundTouchTime.getOrDefault(playerId, 0L) > 200) {
                    // Reset toàn bộ vận tốc khi tiếp đất
                    speedFactors.put(playerId, 0.0); // Reset tốc độ về 0 để tránh bị đẩy
                    player.setVelocity(new Vector(0, player.getVelocity().getY(), 0)); // Reset vận tốc hoàn toàn
                }
            }
        }.runTaskLater(this, 4L); // 0.2 giây = 4 tick
    }

    private void handlePlayerSpeed(Player player, UUID playerId) {
        // Lấy góc yaw hiện tại của người chơi
        float currentYaw = player.getLocation().getYaw();
        float previousYaw = lastYaw.getOrDefault(playerId, currentYaw);

        // Tính toán sự thay đổi góc yaw
        float yawDifference = Math.abs(currentYaw - previousYaw);
        lastYaw.put(playerId, currentYaw);

        // Nếu góc yaw thay đổi lớn (xoay), tăng tốc độ
        if (yawDifference > 2) {
            increaseSpeed(playerId);
        }

        // Lấy vận tốc hiện tại của người chơi và tính lại hướng vận tốc
        Vector velocity = player.getVelocity();

        // Reset hoàn toàn vận tốc trên trục X và Z trước khi tính toán lại
        velocity.setX(0);
        velocity.setZ(0);

        // Tính toán hướng vận tốc của người chơi dựa trên yaw
        double radians = Math.toRadians(currentYaw);
        double x = -Math.sin(radians); // Hướng X
        double z = Math.cos(radians);  // Hướng Z

        // Lấy tốc độ hiện tại của người chơi và áp dụng tốc độ tăng dần
        double currentSpeed = speedFactors.getOrDefault(playerId, 0.0); // Tốc độ mặc định 0 khi vừa chạm đất

        // Áp dụng vận tốc mới cho người chơi
        velocity.setX(x * currentSpeed);
        velocity.setZ(z * currentSpeed);

        // Cập nhật vận tốc của người chơi, giữ nguyên chiều cao (Y)
        player.setVelocity(new Vector(velocity.getX(), player.getVelocity().getY(), velocity.getZ()));
    }

    private void increaseSpeed(UUID playerId) {
        // Lấy tốc độ hiện tại và tăng nhẹ tốc độ mỗi khi xoay
        double currentSpeed = speedFactors.getOrDefault(playerId, 0.0); // Tốc độ mặc định là 0 khi chưa tăng tốc
        currentSpeed += 0.01; // Tăng tốc độ nhẹ

        // Cập nhật tốc độ người chơi
        speedFactors.put(playerId, currentSpeed);
    }
}
// lỗi không reset vận tốc, khi chạm đất hơn 0.2s, khi chạy nhảy lại thì bị đẩy bởi vận tốc cú
