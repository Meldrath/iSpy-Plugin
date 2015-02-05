/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lsd.umc.nodeka.plugin;

import com.lsd.umc.script.ScriptInterface;
import com.lsd.umc.util.AnsiTable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class iSpy {

    //$i[L:$L] $T: $t vs. $O: $o$i$I[L:$L] R:$r X:#x G:$g A:$a$I$RH:$h/$H M:$m/$M S:$s/$S E:$e/$E $p ->
    private ScriptInterface script;
    private String chatChannel;
    private String ispy = "ispy";
    private static final Pattern query = Pattern.compile("^\\[.+:.+\\]: 'gettrig'$");
    private static final Pattern target = Pattern.compile("\033\\[1;37m\\(\033\\[0m\033\\[0m\033\\[37m (immoral|moral|true impartial|impartial)(?:, (?:invisible|cloaked)){0,2} (?:.+(Comatose|AFK))?.+(?:\033\\[1;37m(.+))\033\\[37m.*(is standing here|is here, passed out|is resting here|is here, fighting)(?:(.+))*\\.\033\\[0m");
    private static final Pattern nonCombatPrompt = Pattern.compile("^(?:\\[?(?:Lag|L):\\d+\\])?\\s?(?:\\[?(?:Reply|R)\\[?)");
    private static final Pattern combatPrompt = Pattern.compile("^(?:\\[?(?:Lag|L):\\d+\\])?\\s?(?:.+):\\s?\\((?:.+)\\)");

    //Mora   Obsidian G   Anointed Fall   Runia     < aug-1 > Syskosis
    //Mora   Jadior of    Stone-guard     Blade     < mentor > Trebax
    //Immo   Storm Wiel   Grey-forest H   Hugs      Exuro
    //Immo   Naj'rei Fa   Meijin Joufu    Ursa      Stockmos
    //Tr I   Ice Drakon   Second Order    Ursa      ( AFK ) Dalimar
    
    //private static final Pattern who = Pattern.compile("^(?:");
    //private static final Pattern death = Pattern.compile("falls to the ground! .+? is dead!$|flies by you.$");
    private static final Pattern clairvoyance = Pattern.compile("^You see a vision in your mind\\.{3}");
    private static final Pattern shadowGaze = Pattern.compile("^You assemble the incantation of, 'ycaj kafg\\.'");
    private static final Pattern astralTravel = Pattern.compile("^You chant the words, 'mu xogmxogyx\\.'");
    private static final String DATE_FORMAT_NOW = "hh:mm:ss z";
    private boolean spyGrab = false;
    private boolean wait = false;
    private boolean sixth = true;
    private boolean combat;
    private boolean spying;
    private boolean spied;
    private boolean verbose;
    private String name;
    private String spySpell = "clair ";
    private final Queue spyLines = new LinkedList();
    private final Queue queue = new LinkedList();

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public void init(ScriptInterface script) {
        this.script = script;
        
        this.chatChannel = "ct ";

        this.script.print(AnsiTable.getCode("yellow") + "iSpy Plugin loaded.\001");
        this.script.registerCommand("spy", "com.lsd.umc.nodeka.plugin.iSpy", "menu");
    }

    public String menu(String args) {
        List<String> argArray = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(args);
        while (m.find()) {
            argArray.add(m.group(1).replace("\"", ""));
        }

        if (argArray.isEmpty() || argArray.size() > 2 || "".equals(argArray.get(0))) {
            this.script.print(AnsiTable.getCode("yellow") + "Syntax:\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "help" + AnsiTable.getCode("white") + "\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "channel" + AnsiTable.getCode("white") + " <channel>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "set" + AnsiTable.getCode("white") + " <astral|clair>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "astral" + AnsiTable.getCode("white") + " <target>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "clair" + AnsiTable.getCode("white") + " <target>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "trigger" + AnsiTable.getCode("white") + " <spyCommand>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "verbose" + AnsiTable.getCode("white") + " <on|off>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "sixthCheck" + AnsiTable.getCode("white") + " <on|off>\001");
            this.script.print(AnsiTable.getCode("white") + " > #spy " + AnsiTable.getCode("yellow") + "clear" + AnsiTable.getCode("white") + "\001");
            return "";
        }

        if ("help".equals(argArray.get(0))) {
            this.script.print(AnsiTable.getCode("yellow") + "Help/Tips:\001");
            this.script.print(AnsiTable.getCode("white") + "   > Set your spy spell with #spy set <spell>! \001");
            this.script.print(AnsiTable.getCode("white") + "   > Set your output with the command #spy channel <channel>! \001");
            this.script.print(AnsiTable.getCode("white") + "TIP: Spies are set to ct by default, to set to reply or tell use #spy channel tell.\001");
            this.script.print(AnsiTable.getCode("white") + "TIP: To set it to your output use #spy channel off.\001");
            this.script.print(AnsiTable.getCode("white") + "TIP: To use your own spies and have the plugin output the spy to the specified channel use #spy <spell> <target>.\001");
            this.script.print(AnsiTable.getCode("white") + "TIP: To change the trigger that your clan can use to activate your spies use #spy trigger <new trigger> (Note this can only be one word!)\001");
            this.script.print(AnsiTable.getCode("white") + "TIP: For your clan's convienance, to query your new spy trigger they can use the following: ct gettrig\001");
        }
        
        if ("debug".equals(argArray.get(0)))
        {
            this.script.capture(String.valueOf(spied));
            this.script.capture(String.valueOf(spying));
            this.script.capture(String.valueOf(combat));
            this.script.capture(String.valueOf(spyGrab));
        }

        if ("channel".equals(argArray.get(0))) {
            if (null != argArray.get(1)) {
                switch (argArray.get(1)) {
                    case "off":
                        this.chatChannel = "#output ";
                        break;
                    case "tell":
                        this.chatChannel = "reply ";
                        break;
                    default:
                        this.chatChannel = argArray.get(1) + " ";
                        break;
                }
            }
        }

        if ("clear".equals(argArray.get(0))) {
            clearSpy(queue);
        }

        if ("sixthCheck".equals(argArray.get(0))) {
            if (null != argArray.get(1)) {
                switch (argArray.get(1)) {
                    case "on":
                        this.sixth = true;
                        break;
                    case "off":
                        this.sixth = false;
                        break;
                    default:
                        this.sixth = true;
                        break;
                }
            }
        }

        if ("verbose".equals(argArray.get(0))) {
            if (null != argArray.get(1)) {
                switch (argArray.get(1)) {
                    case "on":
                        this.verbose = true;
                        break;
                    case "off":
                        this.verbose = false;
                        break;
                    default:
                        this.verbose = false;
                        break;
                }
            }
        }

        if ("trigger".equals(argArray.get(0))) {
            if (null != argArray.get(1)) {
                ispy = argArray.get(1);
            }
        }

        if ("setSpell".equals(argArray.get(0))) {
            if (null != argArray.get(1)) {
                switch (argArray.get(1)) {
                    case "clair":
                        this.spySpell = "clair ";
                        break;
                    case "astral":
                        this.spySpell = "c 'astral' ";
                        break;
                    default:
                        this.spySpell = argArray.get(1);
                        break;
                }
            }
        }

        if ("clair".equals(argArray.get(0))) {
            this.queue.add(argArray.get(1));
            spyGrab = true;
            this.script.send("");
            return "";
        }

        if ("astral".equals(argArray.get(0))) {
            this.queue.add(argArray.get(1));
            spyGrab = true;
            this.script.send("");
            return "";
        }
        return "";
    }

    public void clearSpy(Queue q) {
        wait = false;
        q.stream().forEach((object) -> {
            q.remove(object);
        });
    }

    public boolean checkWho(String name, ScriptInterface event) {
        /*
        Matcher m = nonCombatPrompt.matcher(event.getText());
        HashMap<String, Integer> excludedClasses = new HashMap<>();
        excludedClasses.put("Orange-cloud", 1);
        script.parse("who " + name);
        for(String s : excludedClasses.)
        {
        }
        */
        return true;
    }

    public String reportSpy(Queue spyLines) throws IOException {
        LinkedList<String> remainingLines = new LinkedList<>();
        LinkedList<String> individual = new LinkedList<>();
        String output = "";

        if (verbose) {
            spyLines.stream().forEach((Object object) -> {
                if (!(object.toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", "").equals(clairvoyance.toString())
                        || object.toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", "").equals(astralTravel.toString())
                        || object.toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", "").equals(shadowGaze.toString()))) {
                    this.script.parse(this.chatChannel + object.toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", ""));
                }
            });

            clearSpy(spyLines);
        }

        if (spyLines.peek().toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", "").contains("There is no one in the world by that name.")) {
            this.script.parse(this.chatChannel + "iSpy: " + name + " is cloaked, invisible, or logged out.");
            clearSpy(spyLines);
        }

        if (spyLines.peek().toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", "").contains("You see a vision in your mind...but it fades.")) {
            this.script.parse(this.chatChannel + "iSpy: " + name + " has !spy.");
            clearSpy(spyLines);
        }

        spyLines.stream().forEach((object) -> {
            Matcher t = target.matcher((String) object);
            if (t.find()) {
                /*
                 for (int i = 1; i <= t.groupCount(); i++) 
                 {
                 fullPlayer += t.group(i) + " ";
                 }
                 */
                if (!t.group(3).toLowerCase().contains(name)) {
                    individual.add(t.group(3) + " ");
                }
            } else {
                remainingLines.add(object.toString().replaceAll("\033\\[\\d+;\\d+m", "").replaceAll("\033\\[\\d+m", ""));
            }
        });

        String compactExits = "";

        for (String s : remainingLines) {
            Matcher roomName = Pattern.compile("(.+)(\\[ exits:(.+)\\])").matcher(s);
            Matcher exits = Pattern.compile("\\[ exits: ([\\(\\)\\w\\s]+) \\]$|^\\[Exits: ([\\(\\)\\w\\s]+)\\]").matcher(s);
            Matcher fail = Pattern.compile("You lose your concentration.").matcher(s);

            if (fail.find()) {
                this.script.parse(this.chatChannel + "iSpy: Lost concentration on spy attempt.");
                clearSpy(spyLines);
            }
            if (roomName.find()) {
                compactExits += roomName.group(1) + "[Exit: ";
            }
            if (exits.find()) {
                if (exits.group(1) != null) {
                    for (String split : exits.group(1).split("\\s")) {
                        switch (split) {
                            case "north":
                                compactExits += "N ";
                                break;
                            case "south":
                                compactExits += "S ";
                                break;
                            case "west":
                                compactExits += "W ";
                                break;
                            case "east":
                                compactExits += "E ";
                                break;
                            case "up":
                                compactExits += "U ";
                                break;
                            case "down":
                                compactExits += "D ";
                                break;
                            case "(north)":
                                compactExits += "(N) ";
                                break;
                            case "(south)":
                                compactExits += "(S) ";
                                break;
                            case "(west)":
                                compactExits += "(W) ";
                                break;
                            case "(east)":
                                compactExits += "(E) ";
                                break;
                            case "(up)":
                                compactExits += "(U) ";
                                break;
                            case "(down)":
                                compactExits += "(D) ";
                                break;
                            default:
                                compactExits += "null ";
                                break;
                        }
                    }
                } else {
                    for (String split : exits.group(2).split("\\s")) {
                        switch (split) {
                            case "north":
                                compactExits += "N ";
                                break;
                            case "south":
                                compactExits += "S ";
                                break;
                            case "west":
                                compactExits += "W ";
                                break;
                            case "east":
                                compactExits += "E ";
                                break;
                            case "up":
                                compactExits += "U ";
                                break;
                            case "down":
                                compactExits += "D ";
                                break;
                            case "(north)":
                                compactExits += "(N) ";
                                break;
                            case "(south)":
                                compactExits += "(S) ";
                                break;
                            case "(west)":
                                compactExits += "(W) ";
                                break;
                            case "(east)":
                                compactExits += "(E) ";
                                break;
                            case "(up)":
                                compactExits += "(U) ";
                                break;
                            case "(down)":
                                compactExits += "(D) ";
                                break;
                            default:
                                compactExits += "null ";
                                break;
                        }
                    }
                }
            }
        }

        compactExits = compactExits.trim();
        compactExits += "]";

        output += name + " @ " + compactExits + " Room: ";

        if (!individual.isEmpty() || individual.size() != 0) {
            output = individual.stream().map((s) -> s).reduce(output, String::concat);
        } else {
            output += "null";
        }

        this.script.parse(this.chatChannel + output);
        clearSpy(spyLines);
        return "";
    }

    public void IncomingEvent(ScriptInterface event) throws IOException {
        Matcher s = Pattern.compile("^\\[.+:.+\\]: '" + ispy + " (.+)'$").matcher(event.getText());
        Matcher q = query.matcher(event.getText());
        Matcher a = clairvoyance.matcher(event.getText());
        Matcher m = nonCombatPrompt.matcher(event.getText());
        Matcher c = combatPrompt.matcher(event.getText());
        //Matcher d = death.matcher(event.getText());
        
        //gettrig
        if (q.find()) {
            this.script.parse("ct " + ispy);
        }
        
        //We've casted the spy
        if (a.find()
                || "There is no one in the world by that name.".equals(event.getText())
                || "You chant the words, 'mu xogmxogyx.'".equals(event.getText())) {
            spying = true;
        }
        
        //Out of combat
        if (m.find()) {
            combat = false;
        }
        
        //In combat
        if (c.find()) {
            combat = true;
        }
        
        //Non capturing spy
        if (!spyGrab && spying) {
            spying = false;
            this.script.send("");
        }
        
        //Spy at the end of combat
        if (spyGrab && spying) {
            m.reset();
            if (m.find()) {
                spying = false;
                spyGrab = false;
                spied = true;
            }
            if (!spied) {
                this.spyLines.add(event.getEvent());
            }
        }
        
        //Report spy
        if (!spyGrab && !spying && spied) {
            spied = false;
            reportSpy(spyLines);
        }
        if (s.find()) {
            //Adding to the spy queue.
            queue.add(s.group(1).toLowerCase());
        }
        
        //Send the spy command once we're not in combat
        if (!queue.isEmpty() && !combat && !wait) {
            this.script.send("");
            name = (String) queue.peek();
            this.script.parse(this.spySpell + queue.remove()); 
            spyGrab = true;
            wait = true;
        }
    }
}
