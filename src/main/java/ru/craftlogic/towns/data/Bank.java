package ru.craftlogic.towns.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.JsonUtils;
import ru.craftlogic.api.economy.EconomyManager;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.towns.TownManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Bank {
    private final TownManager townManager;
    private final Town town;
    private float limit = Float.MAX_VALUE;
    private float balance;
    private Map<UUID, Account> accounts = new HashMap<>();
    private EconomyManager economy;

    public Bank(TownManager townManager, Town town) {
        this.townManager = townManager;
        this.town = town;
        this.economy = townManager.getServer().getEconomyManager();
    }

    public void load(JsonObject b) {
        this.balance = JsonUtils.getFloat(b, "balance");
        JsonObject accounts = b.getAsJsonObject("accounts");
        for (Map.Entry<String, JsonElement> e : accounts.entrySet()) {
            UUID id = UUID.fromString(e.getKey());
            JsonObject a = e.getValue().getAsJsonObject();
            Account account = new Account(id);
            account.load(a);
            this.accounts.put(id, account);
        }
    }

    public void save(JsonObject b) {
        b.addProperty("balance", this.balance);
        JsonObject accounts = new JsonObject();
        for (Map.Entry<UUID, Account> entry : this.accounts.entrySet()) {
            UUID id = entry.getKey();
            Account account = entry.getValue();
            JsonObject a = new JsonObject();
            account.save(a);
            accounts.add(id.toString(), a);
        }
        b.add("accounts", accounts);
    }

    public float withdraw(float amount) {
        float toWithdraw = Math.max(amount, this.balance);
        this.balance -= amount;
        return toWithdraw;
    }

    public float deposit(float amount) {
        float toDeposit = Math.min(amount, this.limit - this.balance);
        this.balance += toDeposit;
        return toDeposit;
    }

    public float getBalance() {
        return balance;
    }

    public Text<?, ?> getFormattedBalance() {
        return this.economy.format(this.balance);
    }

    public class Account {
        private final UUID player;
        private Deposit deposit;
        private Loan loan;
        private boolean locked;

        public Account(UUID player) {
            this.player = player;
        }

        public void load(JsonObject a) {
            this.locked = JsonUtils.getBoolean(a, "locked", false);
        }

        public void save(JsonObject a) {
            if (this.locked) {
                a.addProperty("locked", true);
            }
        }

        public Bank getBank() {
            return Bank.this;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public class Deposit {
            private final float initial;
            private final float percent;
            private float available;

            public Deposit(float initial, float percent) {
                this.initial = initial;
                this.percent = percent;
            }

            public void load(JsonObject d) {

            }

            public void save(JsonObject d) {

            }

            public float getInitial() {
                return initial;
            }

            public float getPercent() {
                return percent;
            }

            public void setAvailable(float available) {
                this.available = available;
            }

            public float getAvailable() {
                return available;
            }

            public float accrue() {
                if (!Account.this.isLocked()) {
                    float addition = this.initial * this.percent;

                }
                return 0;
            }
        }

        public class Loan {
            private final float initial;
            private final float percent;
            private float rest;

            public Loan(float initial, float percent) {
                this.initial = this.rest = initial;
                this.percent = percent;
            }

            public void load(JsonObject l) {

            }

            public void save(JsonObject l) {

            }

            public float getInitial() {
                return initial;
            }

            public float getPercent() {
                return percent;
            }

            public float repay(float amount) {
                if (!Account.this.isLocked()) {

                }
                return 0;
            }

            public void setRest(float rest) {
                this.rest = rest;
            }

            public float getRest() {
                return rest;
            }
        }
    }
}
