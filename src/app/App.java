package app;

import character.*;
import character.PlayerCharacter;
import magic.Spell;
import items.*;
import utils.Help;
import utils.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class App {
	public static final Long buildNum = 1L;
    static boolean QUIT_ALL = false;
	private static PlayerCharacter activeChar;
	private static String prompt = "CharacterCommand> ";
	private static LinkedHashMap<String, PlayerCharacter> characterList;
	private static Scanner scanner;
	private static String[] input;
    static LinkedList<String> tokens;

	private static PropertiesHandler propertiesHandler;
    private static CommandHandler commandHandler;

    private static void makeTestCharacter(){
		PlayerCharacter frodo;
		frodo = new PlayerCharacter("Frodo Baggins", "Hobbit", "Adventurer");
		frodo.addNewItem(new Consumable("Rations", 5));
		Weapon sting = new Weapon("Sting");
		sting.addEffect(new ItemEffect(frodo.getAbilities().get(Ability.STR), 2));
		Armor mith = new Armor("Mithril Chainmail");
		mith.addEffect(new ItemEffect(frodo.getAbilities().get(Ability.DEX),2));
		mith.setArmorType(Armor.ArmorType.L_ARMOR);
		mith.setAC(15);
		frodo.addNewItem(sting);
		frodo.addNewItem(mith);
		frodo.getSpellBook().learn(new Spell("Invisibility",Spell.CANTRIP));
		sting.equip(frodo);
		mith.equip(frodo);
		characterList.put("frodo baggins", frodo);
	}

	public static void main(String[] args){
		initApp();

		makeTestCharacter();

		while(QUIT_ALL == false){
			if (activeChar != null){
				prompt = "CharacterCommand @ "+activeChar.getName()+"> ";
				if(propertiesHandler.getViewAlways()){
					System.out.println(activeChar);
				}
			}
			System.out.print(prompt);
			input = scanner.nextLine()
					.trim()
					.split("\\s+");
			for (String s:input){
				tokens.add(s);
			}
            commandHandler.doCommand(tokens.peek(), activeChar);
			tokens.clear();
		}
		scanner.close();
	}

	static void setActiveChar(PlayerCharacter pc){
	    activeChar = pc;
    }

/**INITIALIZATION******************************************************************************************************/
	 private static void initApp(){
		propertiesHandler = new PropertiesHandler();
		commandHandler = new CommandHandler();
		checkDirs();
		tokens = new LinkedList<String>();
		scanner = new Scanner(System.in);
		characterList = new LinkedHashMap<String, PlayerCharacter>();
		importAll(false);
	}

/**PREFERENCES*********************************************************************************************************/
    static void prefs(){
		String command = tokens.pop();
		if(!tokens.isEmpty()){
		 	prefs(command);
		} else {
			System.out.println("PREFS");
		}
	}

	private static void prefs(String command){
		while(!tokens.isEmpty()) {
			switch (tokens.peek()) {
				case "-e":
				case "--export":
					tokens.pop();
					File exportFile = Paths.get(tokens.pop()).toFile();
					if(exportFile.isDirectory()){
						propertiesHandler.setExportDir(exportFile.toPath());
					}
					System.out.println("Set export directory to "+exportFile.toString());
					break;
				case "-d":
				case "--data":
					tokens.pop();
					File dataFile = Paths.get(tokens.pop()).toFile();
					if(dataFile.isDirectory()){
						propertiesHandler.setDataDir(dataFile.toPath());
					}
					System.out.println("Set data directory to "+dataFile.toString());
					break;
				case "-v":
				case "--viewAlways":
					tokens.pop();
					if (tokens.peek().equalsIgnoreCase("true") || tokens.peek().equalsIgnoreCase("false")) {
						String token = tokens.pop();
					    propertiesHandler.setViewAlways(Boolean.parseBoolean(token));
						System.out.println("Set 'viewAlways' to "+token);
					} else {
						System.out.println("ERROR: Argument must be 'true' or 'false'");
					}
					break;
				case "--help":
					tokens.pop();
					//TODO: help menu
					System.out.println("PREFS HELP MENU");
					break;
				default:
					if (tokens.peek().startsWith("-")) {
						System.out.println("ERROR: Invalid flag '" + tokens.pop() + "'");
						System.out.println("Enter 'prefs --help' for help");
					} else {

					}
					break;
			}
		}
		propertiesHandler.writeProperties();
	}

/*CHECKDIRS***********************************************************************************************************/
	private static void checkDirs(){
		if (!Files.exists(propertiesHandler.getDataDir())){
			try {
				Files.createDirectories(propertiesHandler.getDataDir());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (!Files.exists(propertiesHandler.getExportDir())){
			try {
				Files.createDirectories(propertiesHandler.getExportDir());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

/**IMPORT**************************************************************************************************************/
	static void importCharacter(){
		tokens.pop();
		String characterName = "";
		while(!tokens.isEmpty()){
			characterName += tokens.pop()+" ";
		}
		characterName = characterName.trim();
		Path charPath = Paths.get(propertiesHandler.getDataDir()+"/"+characterName+".data");
		if(Files.exists(charPath)){
			File charFile = charPath.toFile();
			try {
				ObjectInputStream inStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(charFile)));
				PlayerCharacter playerCharacter = new PlayerCharacter();
				playerCharacter = (PlayerCharacter) inStream.readObject();
				inStream.close();
				if(!characterList.containsKey(playerCharacter.getName())){
					characterList.put(playerCharacter.getName(), playerCharacter);
					System.out.println("All characters imported");
				}else {
					System.out.println(playerCharacter.getName()+" already imported");
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	static void importAll(boolean verbose) {
		File path = propertiesHandler.getDataDir().toFile();
		for(File file:path.listFiles()){
			if (file.isFile()&&file.getName().endsWith(".data")){
		    	try {
					ObjectInputStream inStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
					PlayerCharacter playerCharacter = new PlayerCharacter();
					playerCharacter = (PlayerCharacter) inStream.readObject();
					inStream.close();
					if(!characterList.containsKey(playerCharacter.getName())){
						characterList.put(playerCharacter.getName().toLowerCase(), playerCharacter);
						if(verbose) {
							System.out.println("All characters imported");
						}
					}else {
						System.out.println(playerCharacter.getName()+" already imported");
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		;
	}
	
/**SAVE****************************************************************************************************************/
	static void saveChar(PlayerCharacter c) {
		try {
			String dataFile = propertiesHandler.getDataDir()+"/"+c.getName()+".data";
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
			out.writeObject(c);
			out.close();
			System.out.println("Saved "+c.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

/**LOAD****************************************************************************************************************/
    static void loadChar(){
		String command = tokens.pop();
		if (!tokens.isEmpty()){
			loadChar(command);
		} else {
			System.out.print("Enter name of character to load, or enter 'new' to create a new character: ");
			String characterName = scanner.nextLine().toLowerCase();
			if (characterName.equalsIgnoreCase("new")) {
				createCharacter();
			} else if (!characterName.equalsIgnoreCase("quit")) {
				if (characterList.get(characterName) != null) {
					activeChar = characterList.get(characterName);
					System.out.println(characterName + " loaded");
				} else {
					System.out.println("ERROR: Character not found");
				}
			}
		}
	}

	private static void loadChar(String command){
		String characterName = "";
		while (!tokens.isEmpty()){
			characterName += tokens.pop()+" ";
		}
		characterName = characterName.trim().toLowerCase();
		if (characterList.get(characterName) != null) {
			activeChar = characterList.get(characterName);
			System.out.println(activeChar.getName() + " loaded");
		} else {
			System.out.println("ERROR: Character not found");
		}
	}

/**CREATE CHARACTER****************************************************************************************************/
	static PlayerCharacter createCharacter(){
		System.out.print("Character name: ");
		String name = scanner.nextLine().trim();
        System.out.print("Race: ");
        String raceName = scanner.nextLine();
		System.out.print("Class: ");
		String className = scanner.nextLine();
		PlayerCharacter c = new PlayerCharacter(name, raceName, className);
		for(String key:c.getAbilities().keySet()){
			Ability a = c.getAbilities().get(key);
			a.setBaseVal(getValidInt("Enter "+a.getName()+" score: "));
		}
		characterList.put(c.getName().toLowerCase(), c);
		System.out.println("Created "+c.getName());
		activeChar = c;
		return c;
	}
	
/**SKILLS**************************************************************************************************************/
    static void skills(){
		String command = tokens.pop();
		String action = null;
		if (!tokens.isEmpty()){
			skills(command);
		} else {
			Skill skill;
			boolean exit = false;
			while(!exit){
				System.out.print("View | Train | Forget | Expertise | View All : ");
				action = scanner.nextLine().trim().toLowerCase();
				switch(action){
				case "v":
				case "view":
					skill = getSkillByName();
					System.out.println(skill);
					exit=true;
					break;
				case "t":
				case "train":
					skill = getSkillByName();
					skill.train(activeChar);
					System.out.println("Gained proficiency in "+skill.getName());
					exit=true;
					break;
				case "f":
				case "forget":
					skill = getSkillByName();
					skill.untrain(activeChar);
					System.out.println("Lost proficiency in "+skill.getName());
					exit=true;
					break;
				case "e":
				case "expertise":
					skill = getSkillByName();
					skill.expert(activeChar);
					System.out.println("Gained expertise in "+skill.getName());
					exit=true;
					break;
				case "va":
				case "view all":
					System.out.println("Skills:");
					for(String key:activeChar.getSkills().keySet()){
						System.out.println("- "+activeChar.getSkills().get(key));
					}
					exit=true;
					break;
				case "quit":
					exit=true;
					break;
				default:
					System.out.println(Message.ERROR_SYNTAX);
					System.out.println("Enter 'quit' to quit");
					exit = false;
					break;
				}
				if(exit){
					break;
				}
			}
		}
	}

	private static void skills(String command){
		String skillName = "";
		Skill skill = null;
		boolean expert = false;
		boolean forget = false;
		boolean train = false;
		boolean view = false;
		boolean viewAll = false;
		boolean help = false;
		
		while(!tokens.isEmpty()){
			switch(tokens.peek()){
			case "-e":
			case "--expert":
				expert = true;
				tokens.pop();
				break;
			case "-t":
			case "--train":
				train = true;
				tokens.pop();
				break;
			case "-f":
			case "--forget":
				forget = true;
				tokens.pop();
				break;
			case "-v":
			case "--view":
				view = true;
				tokens.pop();
				break;
			case "-va":
			case "--viewall":
				tokens.pop();
				viewAll =true;
				break;
			case "--help":
				tokens.pop();
				help = true;
				break;
			default:
				if (tokens.peek().startsWith("-")){
					System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
					System.out.println("Enter 'skill --help' for help");
				} else {
					skillName += tokens.pop()+" ";
				}
				break;
			}
		}
		if(help){
            System.out.println(Help.SKILL);
            /*System.out.println("Usage:" +
					"\n  skill" +
					"\n  skill [options, skill_name]");
			System.out.println("Options:" +
					"\n  -e, --expert" +
					"\n  -t, --train" +
					"\n  -f, --forget" +
					"\n  -v, --view" +
					"\n  -va, --viewall" +
					"\n  --help");*/
		} else {
			skillName = skillName.trim().toLowerCase();
			skill = activeChar.getSkills().get(skillName);
			if (viewAll){
				System.out.println("Skills:");
				for(String key:activeChar.getSkills().keySet()){
					System.out.println("- "+activeChar.getSkills().get(key));
				}
			}
			if(skill!=null){
				if (!forget){
					if (expert){
						skill.expert(activeChar);
						System.out.println("Gained expertise in "+skill.getName());
					} else if (train){
						skill.train(activeChar);
						System.out.println("Gained proficiency in "+skill.getName());
					}
				} else {
					skill.untrain(activeChar);
					System.out.println("Lost proficiency in "+skill.getName());
				}
				if (view){
					System.out.println(skill);
				}
			} else {
				if (skillName.equals("") && !viewAll){
					System.out.println("ERROR: Missing argument: skill name");
				} else {
					if (!viewAll){
						System.out.println(Message.ERROR_SYNTAX);
					}
				}
			}
		}
	}
	
/**LEVELUP*************************************************************************************************************/
	static void levelUp(){
		tokens.pop();
		boolean help = false;
		if (tokens.isEmpty()){
			activeChar.levelUp();
			System.out.println(activeChar.getName()+" is now level "+activeChar.getLevel());
		} else {
			Integer level = null;
			
			while(!tokens.isEmpty()){
				switch(tokens.peek()){
				case "-l":
				case "--level":
					tokens.pop();
					if (tokens.isEmpty()){
						System.out.println(Message.ERROR_NO_ARG+": level");
					} else {
						level = getIntToken();
					}
					break;
				case "--help":
					tokens.pop();
					help = true;
					break;
				default:
					if (tokens.peek().startsWith("-")){
						System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
					} else {
						tokens.pop();
					}
					break;
				}
			}
			if (!help){
				if (level!=null){
					activeChar.level(level);
					System.out.println(activeChar.getName()+" is now level "+activeChar.getLevel());
				} else {
					System.out.println("ERROR: Invalid input");
				}
			} else {
                System.out.println(Help.LEVELUP);
                /*System.out.println("Usage:" +
						"\n  levelup [options]");
				System.out.println("Options:" +
						"\n  -l, --level level_number" +
						"\n  --help");*/
			}
		}
	}
	
/**SPELLS**************************************************************************************************************/
	static void spells(){
		String command = tokens.pop();
		if(!tokens.isEmpty()){
			switch(tokens.peek()){
				case "--learn":
					learn();
					break;
				case "--forget":
					break;
				case "--prep":
					break;
				case "--prepall":
					break;
				case "--unprep":
					break;
				case "--unprepall":
					break;
				case "-v":
				case "--view":
					System.out.println(activeChar.getSpellBook());
					break;
				case "-va":
				case "--viewall":
					System.out.println(activeChar.getSpellBook());
					break;
				case "cast":
					cast();
					break;
				case "--help":
					System.out.println("Usage:" +
							"\n  spell [options]");
					System.out.println("Options:" +
							"\n  --learn" +
							"\n  --forget" +
							"\n  --prep(all)" +
							"\n  --unprep(all)" +
							"\n  -v, --view" +
							"\n  -va, viewall" +
							"\n  --cast" +
							"\n  --help");
					break;
				default:

					break;
			}
		} else {
		}
	}

/**LEARN***************************************************************************************************************/

    static void learn(){

    }

	private static void learn(String command){
		String spellName = "";
		Integer spellLevel = null;
		boolean help=false;
		tokens.pop();
		while(!tokens.isEmpty()){
			switch(tokens.peek()){
				case "-l":
				case "--level":
					tokens.pop();
					if (tokens.isEmpty()){
						System.out.println(Message.ERROR_NO_ARG+": level");
						spellLevel = null;
					} else {
						spellLevel = getIntToken();
					}
					break;
				case "--help":
					tokens.pop();
					help = true;
					break;
				default:
					if (tokens.peek().startsWith("-")){
						System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
					}else {
						spellName += tokens.pop()+" ";
					}
			}
		}
		if(spellLevel == null){
			spellLevel = Spell.CANTRIP;	//default level
		}

		if(!help){
			Spell spell = new Spell(spellName.trim(), spellLevel);
			learnSpell(spell);
		} else {
			System.out.println("Usage:" +
					"\n  spell learn" +
					"\n  spell learn <spell name> [options]");
			System.out.println("Options:" +
					"\n  -l, --level level_number  (default: cantrip)" +
					"\n  --help");
		}
	}

	static void learnSpell(Spell spell){
		activeChar.getSpellBook().learn(spell);
		if(spell.isCantrip()){
			System.out.println("Learned cantrip '"+spell.getSpellName()+"'");
		} else {
			System.out.println("Learned level "+spell.getSpellLevel()+" spell '"+spell.getSpellName()+"'");
		}
	}

/**FORGET**************************************************************************************************************/
	private static void forgetSpell(Spell spell){
		activeChar.getSpellBook().forget(spell);
	}

/**PREP****************************************************************************************************************/
	private static void prepSpell(){

	}

/**UNPREP**************************************************************************************************************/
	private static void unprepSpell(){

	}

/**CAST****************************************************************************************************************/
	static void cast(){
		String command = tokens.pop();
		if (!tokens.isEmpty()){
			cast(command);
		} else {
			Integer castLevel = -1;
			Spell spell = getSpellByName();
			if(!spell.isCantrip()){
				castLevel = getValidInt("Cast at level: ");
			}
			castSpell(spell, "spellName", castLevel);
		}
	}

	private static void cast(String command){
		Spell spell = null;
		String spellName = "";
		Integer castLevel = -1;
		boolean help = false;
		while (!tokens.isEmpty()){
			switch(tokens.peek()){
			case "-l":
			case "--level":
				tokens.pop();
				if (tokens.isEmpty()){
					System.out.println(Message.ERROR_NO_ARG+": level");
					castLevel = null;
				} else {
					castLevel = getIntToken();
				}
				break;
			case "--help":
				help = true;
				tokens.pop();
				break;
			default:
				if (tokens.peek().startsWith("-")){
					System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
				}else {
					spellName += tokens.pop()+" ";
				}
			}
		}
		if(help){
            System.out.println(Help.CAST);
		} else {
			spell = activeChar.getSpell(spellName.trim());
			castSpell(spell, spellName, castLevel);
		}
	}

	private static void castSpell(Spell spell, String spellName, Integer castLevel){
		if(spell!=null && castLevel!=null){
			if (castLevel == -1){
				castLevel = spell.getSpellLevel();
			}
			castLevel = activeChar.cast(spell, castLevel);
			if (spell.isCantrip()){
				System.out.println("Cast '"+spell.getSpellName()+"' as a cantrip");
			} else {
				if (castLevel < 0){
					System.out.println("No level "+(-castLevel)+" spell slots remaining");
				} else {
					System.out.println("Cast '"+spell.getSpellName()+"' at level "+castLevel);
				}
			}
		} else {
			if (spellName.equals("")){
				System.out.println("ERROR: Missing argument: spell name");
			} else if (spell == null){
				System.out.println("No spell by that name");
			}
		}
	}
	
/**HEAL/HURT***********************************************************************************************************/
	static void heal(){
		String command = tokens.pop();
		if (!tokens.isEmpty()){
			heal(command);
		} else {
			Integer amount=null;
			if(command.equals("heal")){
				amount = getValidInt("HP gained: ");
			} else {
				amount = getValidInt("HP lost: ");
			}
			heal(command, amount, false);
		}
	}

	private static void heal(String command){
		Integer amount=null;
		boolean healAll=false;
		boolean help = false;
		
		while(!tokens.isEmpty()){
			switch (tokens.peek()){
			case "-hp":
			case "--health":
				tokens.pop();
				if (tokens.isEmpty()){
					System.out.println(Message.ERROR_NO_ARG+": amount");
				} else {
					amount = getIntToken();
				}
				break;
			case "--all":
				tokens.pop();
				healAll=true;
				break;
			case "--help":
				tokens.pop();
				help = true;
				break;
			default:
				if (tokens.peek().startsWith("-")){
					System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
				} else {
					tokens.pop();
				}
				break;
			}
		}
		if(amount!=null || healAll){
			heal(command, amount, healAll);
		} else {
			if (help){
                if(command.equals("heal")){
                    System.out.println(Help.HEAL);
                }
                if(command.equals("hurt")){
                    System.out.println(Help.HURT);
                }
                /*System.out.println("Usage:" +
						"\n  "+command+
						"\n  "+command+" [options]");
				System.out.println("Options:" +
						"\n  -hp, --health amount_to_heal" +
						"\n  --all" +
						"\n  --help");*/
			} else {
				System.out.println(Message.ERROR_SYNTAX+"\nEnter '"+command+" --help' for help");
			}
		}
	}

	private static void heal(String command, int amount, boolean healAll){
		switch (command){
		case "heal":
			if (healAll){
				activeChar.fullHeal();
				System.out.println("HP fully restored");
			} else {
				activeChar.heal(amount);
				System.out.println(String.format("Gained %d HP", amount));
			}
			break;
		case "hurt":
			if (healAll){
				activeChar.fullHurt();
				System.out.println("No HP remaining");
			} else {
				activeChar.hurt(amount);
				System.out.println(String.format("Lost %d HP", amount));
			}
			break;
		default:
			break;
		}
	}
	
/**EQUIP/DEQUIP********************************************************************************************************/
	static void equip(){
		Item item =null;
		String equipDequip = tokens.pop();
		String itemName = "";
		boolean help = false;
		
		while(!tokens.isEmpty()){
			if (tokens.peek().equals("--help")){
				help = true;
				tokens.pop();
			}
			itemName += tokens.pop()+" ";
		}
		if (!help){
			itemName = itemName.trim();
			if (itemName.equals("")){
				item = getItemByName();
			} else {
				item = activeChar.getItem(itemName);
				if (item == null) {
					System.out.println(Message.MSG_NO_ITEM);
				}
			}
			if (item!=null){
				if (item.isEquippable()){
					if (equipDequip.equals("equip")){
						activeChar.equip(item);
						System.out.println("\""+item.getName()+"\" equipped");
					} else {
						activeChar.dequip(item);
						System.out.println("\""+item.getName()+"\" dequipped");
					}
				} else {
					System.out.println(Message.ERROR_NOT_EQUIP);
				}
			}
		} else {
			System.out.println("Usage:" +
					"\n  "+equipDequip+" [item_name]");
			System.out.println("Options:" +
					"\n  --help");
		}
	}
	
/**GET*****************************************************************************************************************/
	static void get(){
		String command = tokens.pop();
		boolean quit = false;
		if (!tokens.isEmpty()){
			get(command);
		} else {
			String itemName = "";
			String itemType = null;
			Integer itemCount = null;
			ArrayList<ItemEffect> fxList = null;

			while(true){
				System.out.print("Item | Equippable | Weapon | Armor | Consumable: ");
				String s = scanner.nextLine().trim().toLowerCase();
				if(s.equals("quit")){
					quit = true;
					break;
				} else if(checkStringInSet(s, Item.types)){
					itemType = s;
					break;
				} else {
					System.out.println(Message.ERROR_ITEM_TYPE);
				}
			}

			if (!quit){
                System.out.print("Item name: ");
                itemName = scanner.nextLine();

                itemCount = getValidInt("Count: ");

                if((itemType.equals("weapon"))||(itemType.equals("armor"))||(itemType.equals("equippable"))){
                    fxList = new ArrayList<>();
                    Stat fxTgt=null;
                    while(getYN("Add effect? ")){
                        while(true){
                            System.out.println("Effect target: ");
                            String s = scanner.nextLine();
                            if(s.equalsIgnoreCase("cancel")){
                                quit=true;
                            } else {
                                fxTgt = activeChar.getStat(s);
                            }
                            if(fxTgt!=null || quit){
                                break;
                            } else {
                                System.out.println("ERROR: Effect target not found");
                            }
                        }
                        if(!quit){
                            int fxBon = getValidInt("Effect bonus: ");
                            ItemEffect fx = new ItemEffect(fxTgt, fxBon);
                            fxList.add(fx);
                        } else {
                            break;
                        }
                    }
                }
                itemName = itemName.trim();
                switch(itemType){
                    case "item":
                        Item item = new Item(itemName, itemCount);
                        activeChar.addNewItem(item);
                        break;
                    case "consumable":
                        Consumable consumable = new Consumable(itemName, itemCount);
                        activeChar.addNewItem(consumable);
                        break;
                    case "equippable":
                        Equippable equippable = new Equippable(itemName, itemCount);
                        equippable.setEffects(fxList);
                        activeChar.addNewItem(equippable);
                        break;
                    case "weapon":
                        Weapon weapon = new Weapon(itemName, itemCount);
                        weapon.setEffects(fxList);
                        activeChar.addNewItem(weapon);
                        break;
                    case "armor":
                        Armor armor = new Armor(itemName, itemCount);
                        armor.setEffects(fxList);
                        activeChar.addNewItem(armor);
                        break;
                }
                //System.out.println(String.format("Got %dx \"%s\" (%s)", itemCount, itemName, itemType));
                System.out.println(String.format("Got %dx \"%s\"", itemCount, itemName));
			}
		}
	}

	private static void get(String command){
		String itemName = "";
		String itemType = null;
		Integer itemCount = null;
		Integer ac=null;
		Armor.ArmorType at=null;
		ArrayList<ItemEffect> fxList=null;
		boolean quit = false;
		boolean help = false;
		
		while(!tokens.isEmpty()){
			switch (tokens.peek()){
			case "-c":
			case "--count":
				tokens.pop();
				if (tokens.isEmpty()){
					System.out.println(Message.ERROR_NO_ARG+": count");
					itemCount = null;
				} else {
					itemCount = getIntToken();
				}
				break;
			case "-t":
			case "--type":
				tokens.pop();
				if (tokens.isEmpty()){
					System.out.println(Message.ERROR_NO_ARG+": type");
				} else {
					itemType = tokens.pop();
					if (!checkStringInSet(itemType, Item.types)){
						System.out.println(Message.ERROR_ITEM_TYPE);
						quit=true;
					}
				}
				break;
                case "-e":
                case "--enchant":
                case "--effect":
                    if(fxList==null){
                        fxList = new ArrayList<>();
                    }
                    tokens.pop();
                    Stat fxTgt=null;
                    if(!tokens.isEmpty()){
                        fxTgt = activeChar.getStat(tokens.pop());
                    }
                    if(fxTgt!=null){
                        int fxBon = getIntToken();
                        fxList.add(new ItemEffect(fxTgt, fxBon));
                    } else {
                        System.out.println("ERROR: Effect target not found");
                    }
                    break;
                case "-ac":
                case "--armorclass":
                    tokens.pop();
                    ac = getIntToken();
                    break;
                case "-at":
                case "--armortype":
                    tokens.pop();
                    String type="";
                    if (!tokens.isEmpty()){
                        type = tokens.peek().toLowerCase();
                    }
                    switch(type){
                        case"l":
                        case "light":
                            at = Armor.ArmorType.L_ARMOR;
                            tokens.pop();
                            break;
                        case"m":
                        case "medium":
                            at = Armor.ArmorType.M_ARMOR;
                            tokens.pop();
                            break;
                        case "h":
                        case "heavy":
                            at = Armor.ArmorType.H_ARMOR;
                            tokens.pop();
                            break;
                        case "s":
                        case "shield":
                            at = Armor.ArmorType.SHIELD;
                            tokens.pop();
                            break;
                        case "o":
                        case "other":
                            at = Armor.ArmorType.OTHER;
                            tokens.pop();
                            break;
                        default:
                            if (!tokens.peek().startsWith("-")){
                                tokens.pop();
                            }
                            System.out.println("ERROR: Not a valid armor type");
                            break;
                    }
                    break;
			case "--help":
				tokens.pop();
				help = true;
				quit = true;
			break;
			default:
				if (tokens.peek().startsWith("-")){
					System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
				} else {
					itemName += tokens.pop()+" ";
				}
				break;
			}
		}

		/*DEFAULT VALUES****************/
		if (itemCount == null){
			itemCount = 1;
		}
		if (itemType == null){
			itemType = "item";
		}
		/*DEFAULT VALUES****************/
        if (help){
            System.out.println(Help.GET);
        } else {
            if (itemName.equals("")){
                quit = true;
                System.out.println(Message.ERROR_NO_ARG + ": item_name");
            }
            if (!quit){
                itemName = itemName.trim();
                switch (itemType){
                    case "i":
                    case "item":
                        Item item = new Item(itemName, itemCount);
                        activeChar.addNewItem(item);
                        break;
                    case "c":
                    case "consumable":
                        Consumable consumable = new Consumable(itemName, itemCount);
                        activeChar.addNewItem(consumable);
                        break;
                    case "e":
                    case "equippable":
                        Equippable equippable = new Equippable(itemName, itemCount);
                        equippable.setEffects(fxList);
                        activeChar.addNewItem(equippable);
                        break;
                    case "w":
                    case "weapon":
                        Weapon weapon = new Weapon(itemName, itemCount);
                        weapon.setEffects(fxList);
                        activeChar.addNewItem(weapon);
                        break;
                    case "a":
                    case "armor":
                        Armor armor = new Armor(itemName, itemCount);
                        armor.setEffects(fxList);
                        if (at != null){
                            armor.setArmorType(at);
                            if (ac != null && at != Armor.ArmorType.SHIELD){
                                armor.setAC(ac);
                            }
                            activeChar.addNewItem(armor);
                        } else {
                            System.out.println("ERROR: Armor type not specified. Use 'equippable' for generic equipment");
                        }

                        break;
                }
                //System.out.println(String.format("Got %dx \"%s\" (%s)", itemCount, itemName, itemType));
                System.out.println(String.format("Got %dx \"%s\"", itemCount, itemName));
            }
        }
	}

	/*private static void getItem(String itemName, String itemType, int itemCount, ArrayList<ItemEffect> fxList){
		itemName = itemName.trim();
		switch(itemType){
		case "item":
            Item item = new Item(itemName, itemCount);
			activeChar.addNewItem(item);
			break;
		case "consumable":
		    Consumable consumable = new Consumable(itemName, itemCount);
			activeChar.addNewItem(consumable);
			break;
		case "equippable":
		    Equippable equippable = new Equippable(itemName, itemCount);
            equippable.setEffects(fxList);
			activeChar.addNewItem(equippable);
			break;
		case "weapon":
            Weapon weapon = new Weapon(itemName, itemCount);
            weapon.setEffects(fxList);
			activeChar.addNewItem(weapon);
			break;
		case "armor":
		    Armor armor = new Armor(itemName, itemCount);
		    armor.setEffects(fxList);
			activeChar.addNewItem(armor);
			break;
		}
		//System.out.println(String.format("Got %dx \"%s\" (%s)", itemCount, itemName, itemType));
		System.out.println(String.format("Got %dx \"%s\"", itemCount, itemName));
	}*/
	
/**ADD/DROP************************************************************************************************************/
	static void addDrop(){
		Item item = null;
		String addDrop = tokens.pop();
		if (!tokens.isEmpty()){
			addDrop(addDrop);
		} else {
			Integer itemCount = null;
			item = getItemByName();
			itemCount = getValidInt("Amount: ");
			if (item!=null){
				switch (item.getName().toLowerCase()){
				case "pp":
					addDropCoin(Inventory.indexPL, "Platinum", itemCount, false, addDrop);
					break;
				case "gp":
					addDropCoin(Inventory.indexGP, "Gold", itemCount, false, addDrop);
					break;
				case "sp":
					addDropCoin(Inventory.indexSP, "Silver", itemCount, false, addDrop);
					break;
				case "cp":
					addDropCoin(Inventory.indexCP, "Copper", itemCount, false, addDrop);
					break;
				default:
					addDropItem(item, itemCount, false, addDrop);
					break;
				}
			}
		}
	}

	private static void addDrop(String addDrop){
		Item item = null;
		String itemName = "";
		Integer itemCount = null;
		boolean dropAll = false;
		boolean help = false;
		while(!tokens.isEmpty()){
			switch (tokens.peek()){
			case "-c":
			case "-count":
				tokens.pop();
				if (tokens.isEmpty()){
					System.out.println(Message.ERROR_NO_ARG+": count");
				} else {
					itemCount = getIntToken();
				}
				break;
			case "--all":
				tokens.pop();
				dropAll = true;
				break;
			case "--help":
				tokens.pop();
				help = true;
				break;
			default:
				if (tokens.peek().startsWith("-")){
					System.out.println("ERROR: Invalid flag '"+tokens.pop()+"'");
				} else {
					itemName += tokens.pop()+" ";
				}
				break;
			}
		}
		/*DEFAULT VALUES****************/
		if (itemCount == null){
			itemCount = 1;
		}
		/*DEFAULT VALUES****************/
		if(!help){
			itemName = itemName.trim();
			if (itemCount != null || dropAll){
				switch (itemName.toLowerCase()){
				case "pp":
					addDropCoin(Inventory.indexPL, "Platinum", itemCount, dropAll, addDrop);
					break;
				case "gp":
					addDropCoin(Inventory.indexGP, "Gold", itemCount, dropAll, addDrop);
					break;
				case "sp":
					addDropCoin(Inventory.indexSP, "Silver", itemCount, dropAll, addDrop);
					break;
				case "cp":
					addDropCoin(Inventory.indexCP, "Copper", itemCount, dropAll, addDrop);
					break;
				default:
					item = activeChar.getInventory().get(itemName);
					break;
				}
				if (item!=null){
					addDropItem(item, itemCount, dropAll, addDrop);
				}
			} else {
				System.out.println(Message.ERROR_NO_ARG+": count");
			}
		} else {
		    if(addDrop.equals("add")){
                System.out.println(Help.ADD);
            }
            if(addDrop.equals("drop")){
                System.out.println(Help.DROP);
            }
			/*System.out.println("Usage:"
					+ "\n  "+addDrop
					+ "\n  "+addDrop+" [options, item_name]");
			System.out.println("Options:"
					+ "\n  -c, --count x: amount to "+addDrop
					+ "\n  --all: toggles dropping all"
					+ "\n  --help");*/
		}
	}

	private static void addDropCoin(int coinType, String itemName, Integer itemCount, boolean dropAll, String addDrop){
		if (addDrop.equals("drop") && !dropAll){
			itemCount = -itemCount;
			System.out.println(String.format("Dropped %dx %s", -itemCount, itemName));
		}
		if (dropAll){
			activeChar.getInventory().getCurrency(coinType).setCount(0);
			System.out.println(String.format("Dropped all %s", itemName));
		} else {
			activeChar.getInventory().addCurrency(coinType, itemCount);
			if (addDrop.equals("add")){
				System.out.println(String.format("Added %dx %s", itemCount, itemName));
			}
		}
	}

	private static void addDropItem(Item item, Integer itemCount, boolean dropAll, String addDrop){
		if (addDrop.equals("drop") && !dropAll){
			itemCount = -itemCount;
			System.out.println(String.format("Dropped %dx \"%s\"", -itemCount, item.getName()));
		}
		if (dropAll){
			activeChar.dropAllItem(item);
			System.out.println(String.format("Dropped all \"%s\"", item.getName()));
		} else {
			activeChar.addDropItem(item, itemCount);
			if (addDrop.equals("add")){
				System.out.println(String.format("Added %dx \"%s\"", itemCount, item.getName()));
			}
		}
	}
	
/**I/O & UTILITIES*****************************************************************************************************/
	static Integer getIntToken(){
		Integer n = null;
		try {
			if (tokens.isEmpty()){
				System.out.println(Message.ERROR_NO_VALUE);
			} else {
				n = Integer.parseInt(tokens.pop());
			}
		} catch (NumberFormatException e) {
			System.out.println(Message.ERROR_NOT_INT);
		}
		return n; 
	}

	private static boolean checkStringInSet(String in, String[] a){
	    for (String s: a){
			if (in.equals(s)){
				return true;
			}
		}
		return false;
	}

	private static Skill getSkillByName(){
		Skill skill = null;
		String skillName = "";
		while (skill==null){
			System.out.println("Skill name:");
			skillName = scanner.nextLine().trim();
			if(skillName.equalsIgnoreCase("quit")){
				return skill;
			} else {
				skill = activeChar.getSkills().get(skillName);
				if (skill==null){
					System.out.println(Message.MSG_NO_SKILL);
				} else {
					break;
				}
			}
		}
		return skill;
	}

	private static Spell getSpellByName(){
		String spellName;
		Spell spell = null;
		while (spell==null){
				System.out.print("Name: ");
				spellName = scanner.nextLine().trim();
				if(spellName.equalsIgnoreCase("quit")){
					return spell;
				} else {
					spell = activeChar.getSpell(spellName);
					if (spell==null){
						System.out.println(Message.MSG_NO_SPELL);
					} else {
						break;
					}
				}
		}
		return spell;
	}

	private static Item getItemByName(){
		Item item = null;
		String itemName;
		while (item==null){
				System.out.print("Name: ");
				itemName = scanner.nextLine().trim();
				if(itemName.equalsIgnoreCase("quit")){
					return item;
				} else {
					item = activeChar.getItem(itemName);
					if (item==null){
						for (Item i:activeChar.getInventory().getCurrency()){
							if (i.getName().equalsIgnoreCase(itemName)){
								return i;
							}
						}
						System.out.println(Message.MSG_NO_ITEM);
					} else {
						break;
					}
				}
		}
		return item;
	}

	private static int getValidInt(String message){
		int n = 0;
		while (true) {
			System.out.print(message);
			if (scanner.hasNextInt()){
				n = scanner.nextInt();
				scanner.nextLine();
				break;
			} else {
				System.out.println(Message.ERROR_NOT_INT);
				scanner.nextLine();
			}
		}
		return n;
	}

	static void dispCharacterList(){
		int i = 1;
		System.out.println("Characters:");
		for (PlayerCharacter c:characterList.values()){
			System.out.println("["+i+"] "+c.getName());
			i++;
		}
	}

	static boolean getYN(String message){
        while(true) {
            System.out.println(message + "[Y/N]: ");
            String yn = scanner.nextLine();
            if (yn.equalsIgnoreCase("y")){
                return true;
            }
            if (yn.equalsIgnoreCase("n")){
                return false;
            }
        }
    }
	
/**DICE ROLLING********************************************************************************************************/
	static Integer roll(){
		int sides=20;
		int num=1;
		int total=0;
		int mod=0;
		String result="";
		if (input.length==1){
			num = getValidInt("Enter number of dice: ");
			sides = getValidInt("Enter number of sides: ");
			mod = getValidInt("Enter any bonuses: ");
		} else if (input.length==2) {
			if (input[1].matches("(\\d+d\\d+)|(\\d+d\\d+[\\+|\\-]\\d+)")){
				String[] a = input[1].split("(d)|(?=[+|-])");
				num=Integer.parseInt(a[0]);
				sides=Integer.parseInt(a[1]);
				if (a.length==3){
					mod=Integer.parseInt(a[2]);
				}		
			} else {
				System.out.println("Invalid format");
				return 0;
			}
		} else {
			System.out.println("Invalid format");
			return 0;
		}
		Random random = new Random();
		for (int i=0; i<num; i++){
			int val = random.nextInt(sides)+1;
			result += val;
			if (i<num-1){
				result+=" + ";
			}
			total +=val;
		}
		if (mod!=0){
			System.out.println(String.format("Rolling %dd%d%+d", num,sides,mod));
			System.out.println(String.format("%s (%+d) = %d", result,mod,total+mod));
		} else {
			System.out.println(String.format("Rolling %dd%d", num,sides));
			if (num>1){System.out.println(result+" = "+total);}
		}
		return total+mod;
	}

	private static Integer roll(int num, int sides, int mod){
		DiceRoll roll = new DiceRoll(num, sides);
		int total = roll.roll();
		return total+mod;
	}
	

}