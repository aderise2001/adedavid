''' Python_Project module: a module defining a number of useful 
functions:
- user_locale() function : uses a uniform distribution function to get a user's location
- propagation_loss() function requires user's distance and frequency to compute Propagation losses
- fading() function
- penetration_loss() function computes penetration losses attributable to the exterior walls of the mall
- RSLcalculator() function returns RSL values by subtracting all losses from EIRP

'''
import numpy as np
import math
import matplotlib.pyplot as plt
# ----------------------------------------------------------------------------------------------------------
# function to implement user location

def user_locale(BS_position) :
    location = ["Mall", "Parking lot", "Road"]
    location_probabilities = [0.5, 0.3, 0.2]   
    user_loc = np.random.choice(location, p=location_probabilities)
    if (user_loc == "Mall"):
        user_direction = "West"
        user_speed = 1
        a,b = 0,200
        user_distance = a + np.random.uniform(0,1) * (b-a)  
    elif (user_loc == "Parking lot"):
        user_direction = "East"
        user_speed = 1
        a,b = 200,300
        user_distance = a + np.random.uniform(0,1) * (b-a)
    else:
        user_direction = "East"
        user_speed = 15
        a,b = 300,BS_position
        user_distance = a + np.random.uniform(0,1) * (b-a)
    BS_mobile_dist = BS_position - user_distance
    Smallcell_mobile_dist = user_distance
    user_profile = [BS_mobile_dist,Smallcell_mobile_dist,user_direction,user_speed]
    return user_profile
# ------------------------------------------------------------------------------------------------------------
# function to implement propagation loss. some parameters returned the user_locale() function will be required
def propagation_loss(user_distance,Freq):
    H_basestation = 50
    H_small_cell = 10
    H_mobile = 1.7
    cell_height = [H_basestation, H_small_cell]
    pp_loss = []
    for (mobile_distance, cell_height) in zip(user_distance, cell_height):
        a_H_mobile = (1.1*np.log10(Freq) - 0.7)*H_mobile - (1.56*np.log10(Freq) - 0.8)
        P_loss = 69.55 + 26.16*np.log10(Freq) - 13.82*np.log10(cell_height) + (44.9 - 6.55*np.log10(cell_height))*np.log10(mobile_distance / 1000) - a_H_mobile
        pp_loss.append(P_loss)
    return pp_loss

#--------------------------------------------------------------------------------------------------------------------------------------------

def fading() : # function that implements fading computation
    mean, std = 0, 1 # mean and standard deviation
    x = np.random.normal(mean, std, 10)
    y = np.random.normal(mean, std, 10)
    z = x + y*(1j) 
    Z = np.abs(z)
    deep_fade = sorted(Z) # fade values sorted in ascending order
    second_highest_fade = 10*np.log10(deep_fade[8]) #the second highest fade value is chosen
    return second_highest_fade
# -----------------------------------------------------------------------------------------------------------
def penetration_loss(mobile_distance,BS_position) : # function that implements penetration losses caused by exterior walls of the mall
    BS_mobile_dist = mobile_distance[0]
    Smallcell_mobile_dist = mobile_distance[1]
    if BS_mobile_dist <= 200 :
        pen_loss = [0,21]
    elif Smallcell_mobile_dist > 190 and Smallcell_mobile_dist < 200 :
        pen_loss_BS = (BS_mobile_dist - (BS_position - 200)) * 21 / 10
        pen_loss_SC = (Smallcell_mobile_dist - 190) * 21 / 10
        pen_loss = [pen_loss_BS, pen_loss_SC] 
    else:
        pen_loss = [21,0]
    return pen_loss
#-----------------------------------------------------------------------------------------------------------------------


def RSLcalculator(mobile_distance,Dict_shadow_values,shadow_points,BS_position):

    Freq = 1000
    EIRP_basestation = 57
    EIRP_small_cell = 30
    
    list_P_loss = propagation_loss(mobile_distance,Freq)

                       
    #   fading() : function returns the second highest fade value, from 10 samples.
    fade_value = fading()
    
    #   penetration_loss(BS_mobile_dist,Smallcell_mobile_dist) :implements penetration losses caused by exterior walls of the mall
    pen_losses = penetration_loss(mobile_distance,BS_position)
    
    #shadow_losses routine : Function returns shadow values, depending on location and direction
    Smallcell_mobile_distance = mobile_distance[0]
    if (Smallcell_mobile_distance <= 200) :  # We need to test if user is within or outside the mall first.    
        shadowing_BS = Dict_shadow_values[shadow_points] # shadow value from BS direction to Mobile
        shadowing_SC = 0  # no shadowing effect in the mall
        #print(shadowing_BS,shadowing_SC,shadow_points)
    elif (BS_position - Smallcell_mobile_distance) < 10 :
        shadowing_BS = 0 #no shadowing effect in basestation direction
        shadowing_SC = Dict_shadow_values[1]  #
        #print(shadowing_BS,shadowing_SC,shadow_points)
    elif (Smallcell_mobile_distance - 200) // 10 < 10 :
        shadowing_SC = Dict_shadow_values[shadow_points]
        shadowing_BS = Dict_shadow_values[shadow_points - 1]
    else:  
        shadow_point_SC = (Smallcell_mobile_distance - 200) // 10 #number of shadow points before user's location, in small cell direction
        shadowing_SC = Dict_shadow_values[shadow_points - shadow_point_SC]
        shadow_point_BS =  shadow_point_SC   #we count shadow point from reverse direction
        shadowing_BS = Dict_shadow_values[shadow_point_BS]  # corresponding shadow value is returned
    RSL_basestation = EIRP_basestation - list_P_loss[0] + shadowing_BS + fade_value - pen_losses[0]
    RSL_small_cell = EIRP_small_cell - list_P_loss[1] + shadowing_SC + fade_value - pen_losses[1]

    return [RSL_basestation,RSL_small_cell]



#-----------------------------------------------------------------------------------------------------------------------

def plotgraph(BS_position):
    shadow_points = int((BS_position - 200) / 10) #shdow points are situated 10m away from each other
    shadow_value = np.random.normal(0,2,shadow_points)
    Dict_shadow_values = {} # initialize the dictionary that will contain shadow values
    list_shadow_points = range(1,shadow_points + 1)

    for (keys, values) in zip(list_shadow_points, shadow_value) : # shadow values are mapped to shadow points
        Dict_shadow_values[keys] = values 

    i = 0
    MIN_DIST_M = 1
    MAX_DIST_M = (BS_position - 1)
    RSL_list_BS = []
    RSL_list_SC = []
    mobile_distance = []
    while i < 500 :
        MIN_DIST_M = 1
        MAX_DIST_M = 2999
        dist_arr1 = np.logspace(np.log10(MIN_DIST_M), np.log10(MAX_DIST_M), 500)
        mobile_distance = [dist_arr1[(499-i)],dist_arr1[i]]
        RSL = RSLcalculator(mobile_distance,Dict_shadow_values,shadow_points,BS_position)
        RSL_list_BS.append(RSL[0])
        RSL_list_SC.append(RSL[1])
        i += 1
    RSL_list_BS1 = np.array(RSL_list_BS)
    RSL_list_SC1 = np.array(RSL_list_SC)
    plt.semilogx(dist_arr1, RSL_list_BS1)
    plt.semilogx(dist_arr1, RSL_list_SC1)
    plt.title('RSL against distance')
    plt.xlabel('Distance [m]')
    plt.ylabel('-Path loss [dB]')
    plt.axis( [ 0, MAX_DIST_M, -120, 40 ] )
    plt.legend( [ 'Basestation RSL', 'Small cell RSL' ] )
    plt.grid(True)
    return plt.show(block = False)




