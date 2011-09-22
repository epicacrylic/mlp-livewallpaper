package com.overkill.live.pony;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;

import com.overkill.live.pony.Pony.AllowedMoves;
import com.overkill.live.pony.Pony.Directions;
import com.overkill.ponymanager.PonyManager;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

public class MyLittleWallpaperService extends WallpaperService {
	public static final String TAG = "mlpWallpaper";	
	// Settings
	public static final boolean DEBUG_RENDERTIME = false;
	private boolean RENDER_ON_SWIPE = true;	

    public ArrayList<Pony> selectablePonies = new ArrayList<Pony>();
    
    public static Random rand;

    public File localFolder;
    
    //1 = effect name
    //2 = behavior name
    //3 = right image
	//4 = left image
    //5 = duration
    //6 = delay before next
    //7 = location relative to pony, right
    //8 = center of effect, right
	//9 = location relative to pony, left
	//10 = center of effect, left
    //11 = effect follows pony
    public static final int EF_effect_name = 1;
    public static final int EF_behavior_name = 2;
    public static final int EF_right_image = 3;
    public static final int EF_left_image = 4;
    public static final int EF_duration = 5;
    public static final int EF_delay_before_next = 6;
    public static final int EF_location_right = 7;
    public static final int EF_center_right = 8;
    public static final int EF_location_left = 9;
    public static final int EF_center_left = 10;
    public static final int EF_follow = 11;
            
	// Behavior options
	public static final int BO_name = 1;
	public static final int BO_probability = 2;
	public static final int BO_max_duration = 3;
	public static final int BO_min_duration = 4;
	public static final int BO_speed = 5;
	public static final int BO_right_image_path = 6;
	public static final int BO_left_image_path = 7;
	public static final int BO_movement_type = 8;
	public static final int BO_linked_behavior = 9;
	public static final int BO_speaking_start = 10;
	public static final int BO_speaking_end = 11;
	public static final int BO_skip = 12;
	public static final int BO_xcoord = 13;
	public static final int BO_ycoord = 14;
	public static final int BO_object_to_follow = 15;
    
    @Override
    public void onCreate() {    	
        super.onCreate();       
                
        rand = new Random();      
        
        if(isSDMounted())
			localFolder = new File(Environment.getExternalStorageDirectory(), "ponies");
		else
			localFolder = new File(getFilesDir(), "ponies");
        
		try {
			File[] ponyFolders  = localFolder.listFiles(new FileFilter() {				
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			
		    for(File pony : ponyFolders){
		    	Log.i("Pony", "loading folder " + pony.getPath() + " " + pony.exists());
		        selectablePonies.add(createPonyFromFile(pony));
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        
    }
    
    private boolean isSDMounted(){
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}
    
    public Pony createPonyFromFile(File localFolder){
    	Pony newPony = new Pony("Derp");
    	try{
		    String line = "";
		    File iniFile = new File(localFolder, "pony.ini");
		    Log.i("Pony", "loading file " + iniFile.getPath() + " " + iniFile.exists());
		    BufferedReader br = new BufferedReader(new FileReader(iniFile));
		    while ((line = br.readLine()) != null) {		    	
			           if(line.startsWith("'")) continue; //skip comments
			           if(line.toLowerCase().startsWith("name")){ newPony = new Pony(line.substring(5)); continue;}
			           if(line.toLowerCase().startsWith("behavior")){
				           	String[] columns = ToolSet.splitWithQualifiers(line, ",", "\"");
				           	
				           	AllowedMoves movement = AllowedMoves.None;
				           	String linked_behavior = "";
							int xcoord = 0;
							int ycoord = 0;
							boolean skip = false;
				           	
							if (columns[BO_movement_type].trim().equalsIgnoreCase("none")) {
								movement = AllowedMoves.None;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("horizontal_only")) {
								movement = AllowedMoves.Horizontal_Only;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("vertical_only")) {
								movement = AllowedMoves.Vertical_Only;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("horizontal_vertical")) {
								movement = AllowedMoves.Horizontal_Vertical;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("diagonal_only")) {
								movement = AllowedMoves.Diagonal_Only;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("diagonal_horizontal")) {
								movement = AllowedMoves.Diagonal_Horizontal;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("diagonal_vertical")) {
								movement = AllowedMoves.Diagonal_Vertical;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("all")) {
								movement = AllowedMoves.All;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("mouseover")) {
								movement = AllowedMoves.MouseOver;
							} else if (columns[BO_movement_type].trim().equalsIgnoreCase("sleep")) {
								movement = AllowedMoves.Sleep;
							}
							
							if(columns[BO_right_image_path].trim().endsWith(".gif") == false)
								continue;
							
							if (columns.length > BO_linked_behavior) {
								linked_behavior = columns[BO_linked_behavior].trim();
								skip = Boolean.parseBoolean(columns[BO_skip].trim());
								xcoord = Integer.parseInt(columns[BO_xcoord].trim());
								ycoord = Integer.parseInt(columns[BO_ycoord].trim());
							}
							
				            newPony.addBehavior(
				            		columns[BO_name], 
				            		Double.parseDouble(columns[BO_probability]), 
				            		Double.parseDouble(columns[BO_max_duration]), 
				            		Double.parseDouble(columns[BO_min_duration]),
				            		Double.parseDouble(columns[BO_speed]),
				            		localFolder.getPath() + "/" + columns[BO_right_image_path].trim(), 
				            		localFolder.getPath() + "/" + columns[BO_left_image_path].trim(), 
				            		movement, 
				            		linked_behavior, 
				            		skip, 
				            		xcoord, 
				            		ycoord);
				            newPony.linkBehaviors();
				            continue;
			           } // Behavior
			           if(line.toLowerCase().startsWith("effect")){
							String[] columns = ToolSet.splitWithQualifiers(line, ",", "\"");							
							boolean found_behavior = false;
							
							// Try to find the behavior to associate with
							for (Behavior behavior : newPony.behaviors) {
								if (behavior.name.equalsIgnoreCase(columns[EF_behavior_name].replace('"', ' ').trim())) {
									Directions direction_right = Directions.center;
									Directions centering_right = Directions.center;
									Directions direction_left = Directions.center;
									Directions centering_left = Directions.center;
									
									try {
										direction_right = ToolSet.getDirection(columns[EF_location_right]);
										centering_right = ToolSet.getDirection(columns[EF_center_right]);
										direction_left = ToolSet.getDirection(columns[EF_location_left]);
										centering_left = ToolSet.getDirection(columns[EF_center_left]);
									} catch (Exception ex) {
										// Debug output
										System.out.println("Invalid placement direction or centering for effect " + columns[EF_effect_name] + " for pony " + newPony.name + ":\n" + line);
									}
																		
							        // This is where we load the animation image
									String rightimage = localFolder.getPath() + "/" + columns[EF_right_image].trim();
									String leftimage = localFolder.getPath() + "/" + columns[EF_left_image].trim();									
									// Add the effect to the behavior if the image loaded correctly
									behavior.addEffect(columns[EF_effect_name].replace('"', ' ').trim(), rightimage, leftimage, Double.parseDouble(columns[EF_duration].trim()), Double.parseDouble(columns[EF_delay_before_next].trim()), direction_right, centering_right, direction_left, centering_left, Boolean.parseBoolean(columns[EF_follow].trim()));
									found_behavior = true;
									break;
								}
							}
							if (!found_behavior) {
								// Debug output
								System.out.println("Could not find behavior for effect " + columns[1] + " for pony " + newPony.name + ":\n" + line);
							}
			           } // Effect
		    		
		    	}
	        br.close();
	        Log.i("loading", newPony.name + " with " + newPony.behaviors.size() + " Behaviors");
		  	
    	}catch (Exception e) {
			e.printStackTrace();
		}
		return newPony;
    }
        
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new SpriteEngine();
    }

    class SpriteEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
    	private RenderEngine engine;
        private SharedPreferences preferences;

        SpriteEngine() {
        	this.engine = new RenderEngine(getBaseContext(), getSurfaceHolder());            	
            preferences = MyLittleWallpaperService.this.getSharedPreferences(TAG, MODE_PRIVATE);        
            preferences.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(preferences, null);
        }

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			engine.setShowDebugText(sharedPreferences.getBoolean("debug_info", false));
			engine.setMaxFramerate(Integer.valueOf(sharedPreferences.getString("framerate_cap", "10")));
			engine.setScale(Float.valueOf(sharedPreferences.getString("pony_scale", "1.0")));

			RENDER_ON_SWIPE = sharedPreferences.getBoolean("render_on_swipe", true);
			
			// get Background image if we want one
			this.engine.setBackground(sharedPreferences.getInt("background_color", 0xff000000));
			
			String filePath = sharedPreferences.getString("background_image", null);
	        if(sharedPreferences.getBoolean("background_global", false) == false){
	        	this.engine.setBackground((Bitmap) null);
	        }else{
	        	this.engine.setBackground(filePath);
	        }
	        	
	        for(Pony p : this.engine.getPonies()){
	        	p.cleanUp();
	        }
	        
	        this.engine.clearPonies();
	        
	        for(Pony p : selectablePonies){
	        	Log.i(TAG, "do we want \"" + p.name + "\"?");
	        	if(sharedPreferences.getBoolean(p.name, false) == false)
	        		continue;
	        	 this.engine.addPony(p);
	        }
		}  
                        
        @Override
        public void onVisibilityChanged(boolean visible) {
        	this.engine.setVisibility(visible);
        	if(visible){
                this.engine.resume();
            }else{
            	this.engine.pause();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.engine.setFrameSize(width, height);
            this.engine.render();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            this.engine.start();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.engine.stop();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            engine.setOffset(xPixels);     
            if(RENDER_ON_SWIPE) engine.render();
        }
        
        @Override
        public Bundle onCommand(String action, int x, int y, int z,	Bundle extras, boolean resultRequested) {
        	Log.i("onCommand", action);
        	if(action.equals(PonyManager.ACTION_REMOVE_PONY)){
        		Log.i("onCommand", action + " " + extras.getString("pony"));
        		String removedPonyName = extras.getString("pony");
        		for(Pony p : this.engine.getPonies()){
        			if(p.name.equals(removedPonyName)){
        				this.engine.getPonies().remove(p);
        				break;
        			}
        		}
        	}
        	return super.onCommand(action, x, y, z, extras, resultRequested);
        }
        
//        @Override
//        public void onTouchEvent(MotionEvent event) {
//            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            	if(keyLook==false){
//	                touchX = event.getX();
//	                touchY = event.getY();
//	                for(Pony p : activePonies){
//	                	Log.i("Pony[" + p.name + "]", "trigger Touchevent");
//	                	p.setDestination(touchX, touchY);
//	                		/*p.touch();
//	                		drawFrame();
//	                		break;*/
//	                }	              
//            	}else{
//	            	touchX = -1;
//	                touchY = -1;	            		
//            	}
//            } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            	touchX = -1;
//                touchY = -1;
//                keyLook = false;
//            } else {
//            	touchX = -1;
//                touchY = -1;
//            }
//            super.onTouchEvent(event);
//        }

           
    } // End of SpriteEngine
}