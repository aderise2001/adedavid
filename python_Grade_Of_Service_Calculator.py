
# Python application that simulates the downlink coverage of a shopping mall, its parking lot, and the primary road leading to it.
# This script implements several functionalities defined in a module called "Python_Project"


import sys
import numpy as np
#from beautifultable import BeautifulTable
my_path ='C:\\Users\\Ademola\\Desktop\\My Python LAB'
sys.path.append(my_path)  # adding the Python_Project module's path to the module search path
import Python_Project_Functions_definitions as pp # importing the Python_Project module, 'pp' will be the alias for the module name

while True:
    BS_Summary_report = {}
    SC_Summary_report = {}
    BS_list = []
    SC_list = []
    runs = 1
    runtime = input("Enter number of times to run Simulation (q to quit and print RSL Plot)> ")
    if runtime == 'q' :
        break

    else:
        try:
            run_time = int(runtime)
        except :
            print("Oops!  The number is not valid. Try again...")
            continue
        while runs <= run_time :     
            try: 
                BS_position = int(input("Enter Basestation position: 3km or more from the small cell: "))
            except ValueError:
                print("Oops!  The number is not valid. Try again...")
                continue
            try: 
                Total_sim_time = int(input("Enter total simulation time, in hours: ")) 
            except ValueError:
                print("Oops! Invalid input detected. Give it another trial...")
                continue    
            
            #Basic parameters of geometry of the simulation environment
            PARKING_LOT_WIDTH = 100 # parking lot width in metres
            MALL_LEN = 200 # mall length in metres. 10m of the length are in the entry way between two doors.
            TIME_STEP = 1 # Simulation time step size in second
            MALL_EXTWALL_PENETRATION_LOSS = 21 #loss due to wall penetration in dB

            #Basic properties of Basestation
            H_basestation = 50 # height in metres
            EIRP_basestation = 57 # power in dBm
            N_basestation = 30 # Number of traffic channels
            Freq = 1000 # carrier frequency, measured in Megahertz

            #Basic properties of small cell
            H_small_cell = 10 # height in metres
            EIRP_small_cell = 30 # power in dBm
            N_small_cell = 30 # Number of traffic channels

            #User properties definition
            H_mobile = 1.7 #mobile height in metres
            RSL_THRESH = -102 #Mobile Rx threshold in dBm
            N_users = 1000 #Number of users
            call_rate = 1 # one call per hour
            Ave_call_duration = 10 # call duration of 3 minutes per call

            # Other parameters
            v_light = 3 * 10**8  #light velicty, in m/s
            radio_lambda = v_light / Freq
            time_step = 1    

            #-------------------------------------------------------------------------------------------------------------
            

            # ------------------------------------------------------------------------------------------------------------
            # shadow values for various points within the Basestation location, up to the entrance of the mall.
             
            shadow_points = int((BS_position - 200) / 10) #shdow points are situated 10m away from each other
            shadow_value = np.random.normal(0,2,shadow_points)
            Dict_shadow_values = {} # initialize the dictionary that will contain shadow values
            list_shadow_points = range(1,shadow_points + 1)

            for (keys, values) in zip(list_shadow_points, shadow_value) : # shadow values are mapped to shadow points
                Dict_shadow_values[keys] = values 

            #---------------------------------------------------------------------------------------------------------------

            #Statistics relating to the Basestation
            BS_CHANNELS_INUSE = 0
            BS_BLOCKS_CAPACITY = 0
            BS_BLOCKS_POWER = 0
            BS_CALL_ATTEMPTS = 0
            BS_SUCCESSFUL_CALL_CONNECT = 0
            BS_CALL_DROPS = 0
            BS_COMPLETED_CALLS = 0
            BS_SC_HANDOFF_ATTEMPTS = 0
            BS_SC_SUCCESSFUL_HANDOFFS = 0
            BS_SC_FAILED_HANDOFFS = 0

            #Statistics relating to the small cell
            SC_CHANNELS_INUSE = 0
            SC_BLOCKS_CAPACITY = 0
            SC_BLOCKS_POWER = 0
            SC_CALL_ATTEMPTS = 0
            SC_SUCCESSFUL_CALL_CONNECT = 0
            SC_CALL_DROPS = 0   
            SC_COMPLETED_CALLS = 0
            SC_BS_HANDOFF_ATTEMPTS = 0
            SC_BS_SUCCESSFUL_HANDOFFS = 0
            SC_BS_FAILED_HANDOFFS = 0


            BS_table = BeautifulTable() # Python in-built function for table generation
            SC_table = BeautifulTable()
            BS_Summary_table = BeautifulTable()  #simulation summary table for Basestation stats 
            SC_Summary_table = BeautifulTable()  #simulation summary table for small cell stats
            BS_statistics = {}  #dictionary containing Statistics relating to Basestation 
            SC_statistics = {}  #dictionary containing Statistics relating to Basestation  
            BS_established_calllog = [] #list containing calls that are established on the Basestation
            SC_established_calllog = [] #list containing calls that are established on the small cell

            Active_call = 0
            sim_start = 1
            Total_call_request = 0
            while sim_start <= (Total_sim_time*3600) :
                Num_call_request = 0
                Inactive_call = 0
                while Inactive_call < (N_users - Active_call) :
                    user_status = ["call_attempt", "no_call_attempt"]
                    pr_to_call = call_rate * time_step / 3600
                    call_probabilities = [pr_to_call, (1 - pr_to_call)]   
                    callrequest = np.random.choice(user_status, p=call_probabilities)
                
                    if (callrequest == "call_attempt"):
                        
                        # ************************************************************************************************************************
                        #   user_locale() : function requires Basestation position as an argument to return user's location and direction.
                        #   BS_mobile_dist is BS to mobile distance; Smallcell_mobile_dist is Small cell to mobile distance.

                        user_profile = pp.user_locale(BS_position)
                        
                        # ************************************************************************************************************************
                        #   RSL = EIRP - P_losses + shadowing + fade_value - penetration_loss. 
                        # function to compute RSL values is RSLcalculator(mobile_distance,Dict_shadow_values,shadow_points,BS_position)
                        mobile_distance = user_profile[:2]
                        RSL_list = pp.RSLcalculator(mobile_distance,Dict_shadow_values,shadow_points,BS_position)
                        
                        RSL_basestation = RSL_list[0]
                        RSL_small_cell = RSL_list[1]

                        
                        #*********************************************************************************************************************************
                       
                        if (user_profile[1]) > 200 :
                            BS_CALL_ATTEMPTS += 1
                            BS_statistics["BS_CALL_ATTEMPTS"] = BS_CALL_ATTEMPTS
                            if (RSL_basestation >= RSL_THRESH  and N_basestation > len(BS_established_calllog)) :
                                BS_CHANNELS_INUSE += 1
                                call_start = sim_start
                                call_lenght = np.random.exponential(180)
                                call_time_left = call_lenght + call_start
                                BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE  
                                BS_established_calllog.append(["user {}".format(BS_CHANNELS_INUSE),call_time_left,user_profile,RSL_list])
                                
                            elif (RSL_basestation >= RSL_THRESH  and N_basestation <= len(BS_established_calllog)):
                                BS_BLOCKS_CAPACITY += 1
                                BS_statistics["BS_BLOCKS_CAPACITY"] = BS_BLOCKS_CAPACITY
                            
                            else:
                                BS_BLOCKS_POWER += 1
                                BS_statistics["BS_BLOCKS_POWER"] = BS_BLOCKS_POWER
                    
                                if (RSL_small_cell >= RSL_THRESH and N_small_cell > len(SC_established_calllog)) :
                                    SC_CHANNELS_INUSE += 1
                                    call_start = sim_start
                                    call_lenght = np.random.exponential(180)
                                    call_time_left = call_lenght + call_start
                                    SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                                    SC_established_calllog.append(["user {}".format(SC_CHANNELS_INUSE),call_time_left,user_profile,RSL_list])         
                                else:
                                    BS_CALL_DROPS += 1
                                    BS_statistics["BS_CALL_DROPS"] = BS_CALL_DROPS
                                     
                        else:
                            SC_CALL_ATTEMPTS += 1
                            SC_statistics["SC_CALL_ATTEMPTS"] = SC_CALL_ATTEMPTS
                            if (RSL_small_cell >= RSL_THRESH  and N_small_cell > len(SC_established_calllog)) :  
                                SC_CHANNELS_INUSE += 1
                                call_start = sim_start
                                call_lenght = np.random.exponential(180)
                                call_time_left = call_lenght + call_start
                                SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                                SC_established_calllog.append(["user {}".format(SC_CHANNELS_INUSE),call_time_left,user_profile,RSL_list])
                            elif (RSL_small_cell >= RSL_THRESH  and N_small_cell <= len(SC_established_calllog)):
                                SC_BLOCKS_CAPACITY += 1
                                SC_statistics["SC_BLOCKS_CAPACITY"] = SC_BLOCKS_CAPACITY
                            
                            else:
                                SC_BLOCKS_POWER += 1
                                SC_statistics["SC_BLOCKS_POWER"] = SC_BLOCKS_POWER
                            
                                if (RSL_basestation >= RSL_THRESH and N_basestation > len(BS_established_calllog)) :
                                    BS_CHANNELS_INUSE += 1
                                    BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE
                                    call_start = sim_start
                                    call_lenght = np.random.exponential(180)
                                    call_time_left = call_lenght + call_start
                                    BS_established_calllog.append(["user {}".format(BS_CHANNELS_INUSE),call_time_left,user_profile,RSL_list])   
                                else:
                                    SC_CALL_DROPS += 1
                                    SC_statistics["SC_CALL_DROPS"] = SC_CALL_DROPS

                        for user_BS in BS_established_calllog :
                            if (user_BS[1] < 1 or  user_BS[2][0] <= 15 ):
                                BS_COMPLETED_CALLS += 1
                                BS_statistics["BS_COMPLETED_CALLS"] = BS_COMPLETED_CALLS
                                BS_established_calllog.remove(user_BS)
                                BS_CHANNELS_INUSE -= 1
                                BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE
                                continue
                            else:
                                if user_BS[2][2] == "West" and user_BS[2][1] > 300 :
                                    user_BS[2][3] = 3
                                    user_BS[2][0] -= (1 * user_BS[2][3])
                                    user_BS[2][1] += (1 * user_BS[2][3])
                                elif user_BS[2][2] == "West" and user_BS[2][1] <= 300:
                                    user_BS[2][0] -= (1 * user_BS[2][3])
                                    user_BS[2][1] += (1 * user_BS[2][3])
                                elif user_BS[2][1] <= 300 and user_BS[2][2] == "East":
                                    user_BS[2][3] = 1
                                    user_BS[2][0] += (1 * user_BS[2][3])
                                    user_BS[2][1] -= (1 * user_BS[2][3])
                                else:
                                    user_BS[2][0] += (1 * user_BS[2][3])
                                    user_BS[2][1] -= (1 * user_BS[2][3])
                            user_BS[1] -= 1
                            RSL_new_BS = pp.RSLcalculator(user_BS[2][:2],Dict_shadow_values,shadow_points,BS_position)
                            RSL_basestation_new = RSL_new_BS[0]
                            RSL_small_cell_new = RSL_new_BS[1]
                            user_BS[3] = RSL_new_BS
                            if RSL_basestation_new < RSL_THRESH :
                                BS_CALL_DROPS += 1
                                BS_statistics["BS_CALL_DROPS"] = BS_CALL_DROPS
                                BS_established_calllog.remove(user_BS)
                                BS_CHANNELS_INUSE -= 1
                                BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE
                            elif RSL_basestation_new >= RSL_THRESH and RSL_small_cell_new > RSL_basestation_new :       
                                BS_SC_HANDOFF_ATTEMPTS += 1
                                BS_statistics["BS_SC_HANDOFF_ATTEMPTS"] = BS_SC_HANDOFF_ATTEMPTS
                                if N_small_cell > len(SC_established_calllog) : 
                                    BS_SC_SUCCESSFUL_HANDOFFS += 1
                                    BS_statistics["BS_SC_SUCCESSFUL_HANDOFFS"] = BS_SC_SUCCESSFUL_HANDOFFS
                                    BS_CHANNELS_INUSE -= 1
                                    BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE
                                    BS_established_calllog.remove(user_BS)
                                    SC_CHANNELS_INUSE += 1
                                    SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                                    SC_established_calllog.append(user_BS)
                                else:
                                    BS_SC_FAILED_HANDOFFS += 1
                                    BS_statistics["BS_SC_FAILED_HANDOFFS"] = BS_SC_FAILED_HANDOFFS
                                    SC_BLOCKS_CAPACITY += 1
                                    SC_statistics["SC_BLOCKS_CAPACITY"] = SC_BLOCKS_CAPACITY
                            else:
                                continue
                        for user_SC in SC_established_calllog :
                            if (user_SC[1] < 1 or user_SC[2][1] <= 1):
                                SC_COMPLETED_CALLS += 1
                                SC_statistics["SC_COMPLETED_CALLS"] = SC_COMPLETED_CALLS
                                SC_established_calllog.remove(user_SC)
                                SC_CHANNELS_INUSE -= 1
                                SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                                continue
                            else:
                                if user_SC[2][1] > 200 :
                                    user_SC[2][3] = 1
                                else:
                                    user_SC[2][3] = user_SC[2][3]
                                if user_SC[2][2] == "East" :
                                    user_SC[2][0] += (1 * user_SC[2][3])
                                    user_SC[2][1] -= (1 * user_SC[2][3])
                                else:
                                    user_SC[2][0] -= (1 * user_SC[2][3])
                                    user_SC[2][1] += (1 * user_SC[2][3])
                            user_SC[1] -= 1
                            RSL_new_SC = pp.RSLcalculator(user_SC[2][:2],Dict_shadow_values,shadow_points,BS_position)
                        
                            RSL_basestation_new = RSL_new_SC[0]
                            RSL_small_cell_new = RSL_new_SC[1]
                            user_SC[3] = RSL_new_SC
                            if RSL_small_cell_new < RSL_THRESH :
                                SC_CALL_DROPS += 1
                                SC_statistics["SC_CALL_DROPS"] = SC_CALL_DROPS
                                SC_established_calllog.remove(user_SC)
                                SC_CHANNELS_INUSE -= 1
                                SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                            elif RSL_small_cell_new >= RSL_THRESH and RSL_basestation_new > RSL_small_cell_new :
                                SC_BS_HANDOFF_ATTEMPTS += 1
                                SC_statistics["SC_BS_HANDOFF_ATTEMPTS"] = SC_BS_HANDOFF_ATTEMPTS
                                if N_basestation > len(BS_established_calllog) :
                                    SC_BS_SUCCESSFUL_HANDOFFS += 1
                                    SC_statistics["SC_BS_SUCCESSFUL_HANDOFFS"] = SC_BS_SUCCESSFUL_HANDOFFS
                                    SC_CHANNELS_INUSE -= 1
                                    SC_statistics["SC_CHANNELS_INUSE"] = SC_CHANNELS_INUSE
                                    SC_established_calllog.remove(user_SC)
                                    BS_CHANNELS_INUSE += 1
                                    BS_statistics["BS_CHANNELS_INUSE"] = BS_CHANNELS_INUSE
                                    BS_established_calllog.append(user_SC)
                                else:
                                    SC_BS_FAILED_HANDOFFS += 1
                                    SC_statistics["SC_BS_FAILED_HANDOFFS"] = SC_BS_FAILED_HANDOFFS
                                    BS_BLOCKS_CAPACITY += 1
                                    BS_statistics["BS_BLOCKS_CAPACITY"] = BS_BLOCKS_CAPACITY    
                            else:
                                continue                 
                    else:
                        Inactive_call += 1  


                    
                # routine simulates moving mobile users. locations are updated before the next time step since priority is given to this group
                #-----------------------------------------------------------------------------------------------------------------------------------
                
                        
                #Users are connected to either BS or small cell and are moving for potential handoff. RSLs at new locations are calculated
                #-------------------------------------------------------------------------------------------------------------------------    
                
                Active_call = len(BS_established_calllog) + len(SC_established_calllog)   # counter that monitors active calls     
                sim_start += 1
            BS_table.column_headers = [parameter for parameter in BS_statistics]
            BS_table.append_row([BS_statistics[parameter] for parameter in BS_statistics])
            SC_table.column_headers = [parameter for parameter in SC_statistics]
            SC_table.append_row([SC_statistics[parameter] for parameter in SC_statistics])
            
            print("----------------------------------Result for run {}-----------------------------------------------------*".format(runs))

            print(BS_table)
            
            print(SC_table)
            print("----------------------------------------------------------------------------------------------------------*")
            for key in BS_statistics :
                if key in BS_Summary_report :
                    BS_Summary_report[key] = BS_Summary_report[key] + BS_statistics[key]
                else:
                    BS_Summary_report[key] = BS_statistics[key]
                    
            for key in SC_statistics :
                if key in SC_Summary_report :
                    SC_Summary_report[key] = SC_Summary_report[key] + SC_statistics[key]
                else:
                    SC_Summary_report[key] = SC_statistics[key]            
            runs += 1
        BS_Summary_table.column_headers = [parameter for parameter in BS_Summary_report]
        BS_Summary_table.append_row([BS_Summary_report[parameter] for parameter in BS_Summary_report])
        SC_Summary_table.column_headers = [parameter for parameter in SC_Summary_report]
        SC_Summary_table.append_row([SC_Summary_report[parameter] for parameter in SC_Summary_report])
        BS_Summary_table.top_border_char = '='
        BS_Summary_table.bottom_border_char = '='
        SC_Summary_table.top_border_char = '='
        SC_Summary_table.bottom_border_char = '='
        try:
            smallcell_calldrop = SC_Summary_report["SC_CALL_DROPS"]
        except KeyError:
            smallcell_calldrop = 0
        try:
            basestation_calldrop = BS_Summary_report["BS_CALL_DROPS"]
        except KeyError :
            basestation_calldrop = 0
        total_call_drops = smallcell_calldrop + basestation_calldrop
        total_call_attepmts = BS_Summary_report["BS_CALL_ATTEMPTS"] + SC_Summary_report["SC_CALL_ATTEMPTS"]
        percent_of_call_drops = total_call_drops / total_call_attepmts
        
        
        
        print("*------------------------"  +  " BS Summary Report " + "------------------------------------------------------*")

        
        print(BS_Summary_table)


        print("*------------------------"  +  " SC Summary Report " + "-----------------------------------------------------*")


        print(SC_Summary_table)


        print("-------------------------------------------------------------------------------------------------------------*")


        print("The total number of dropped calls is: {}".format(total_call_drops))


        print("-------------------------------------------------------------------------------------------------------------*")


        print("The percentage of call attempts that ended as dropped calls is: {}".format(percent_of_call_drops))


        print("*------------------------"  +  " End of Summary Report " + "-------------------------------------------------*")
        

#-------------------------------------------------------------------------------------------------------------------------------        
#--------------------------------------------RSL_DISTANCE PLOT SECTION----------------------------------------------------------
    
BS_position = 3000
RSLgraph = pp.plotgraph(BS_position)

    
        










                





